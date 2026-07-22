import { Hono } from 'hono';
import { cors } from 'hono/cors';

interface Env {
  DB: D1Database;
  GITHUB_OWNER: string;
  GITHUB_REPO: string;
  GITHUB_STAR_REPO: string;
  GITHUB_TOKEN: string;
}

interface Paper {
  id: string;
  title: string;
  course: string;
  folder: string;
  year: number;
  school: string;
  path: string;
  size: number;
  device_id: string;
  downloads: number;
  created_at: number;
}

const app = new Hono<{ Bindings: Env }>();

app.use('*', cors());

// Get all papers with optional filters
app.get('/papers', async (c) => {
  const folder = c.req.query('folder');
  const course = c.req.query('course');
  const q = c.req.query('q');

  let query = 'SELECT * FROM papers WHERE 1=1';
  const params: any[] = [];

  if (folder) {
    query += ' AND folder = ?';
    params.push(folder);
  }
  if (course) {
    query += ' AND course = ?';
    params.push(course);
  }
  if (q) {
    query += ' AND (title LIKE ? OR course LIKE ?)';
    params.push(`%${q}%`, `%${q}%`);
  }

  query += ' ORDER BY created_at DESC';

  const result = await c.env.DB.prepare(query).bind(...params).all<Paper>();
  return c.json(result.results);
});

// Get unique courses
app.get('/courses', async (c) => {
  const result = await c.env.DB.prepare(
    'SELECT DISTINCT course FROM papers ORDER BY course'
  ).all<{ course: string }>();
  return c.json(result.results.map((r) => r.course));
});

// Get unique folders
app.get('/folders', async (c) => {
  const result = await c.env.DB.prepare(
    'SELECT DISTINCT folder FROM papers ORDER BY folder'
  ).all<{ folder: string }>();
  return c.json(result.results.map((r) => r.folder));
});

// Get paper detail
app.get('/papers/:id', async (c) => {
  const id = c.req.param('id');
  const result = await c.env.DB.prepare(
    'SELECT * FROM papers WHERE id = ?'
  ).bind(id).first<Paper>();

  if (!result) {
    return c.json({ error: 'Paper not found' }, 404);
  }
  return c.json(result);
});

// Download paper (proxy download from GitHub)
app.get('/download/:id', async (c) => {
  const id = c.req.param('id');
  const result = await c.env.DB.prepare(
    'SELECT * FROM papers WHERE id = ?'
  ).bind(id).first<Paper>();

  if (!result) {
    return c.json({ error: 'Paper not found' }, 404);
  }

  await c.env.DB.prepare(
    'UPDATE papers SET downloads = downloads + 1 WHERE id = ?'
  ).bind(id).run();

  const rawUrl = `https://raw.githubusercontent.com/${c.env.GITHUB_OWNER}/${c.env.GITHUB_REPO}/main/${result.path}`;
  const ghResponse = await fetch(rawUrl, {
    headers: {
      'User-Agent': 'ChrnovaPapers-Worker',
      'Authorization': `token ${c.env.GITHUB_TOKEN}`,
    },
  });

  if (!ghResponse.ok) {
    return c.json({ error: 'Failed to fetch from GitHub' }, 500);
  }

  return new Response(ghResponse.body, {
    headers: {
      'Content-Type': 'application/pdf',
      'Content-Disposition': `attachment; filename="${result.title}.pdf"`,
    },
  });
});

// Upload paper
app.post('/upload', async (c) => {
  try {
    const deviceId = c.req.header('X-Device-Id');
    if (!deviceId) {
      return c.json({ error: 'X-Device-Id header required' }, 400);
    }

    const formData = await c.req.formData();
    const file = formData.get('file') as File | null;
    const title = formData.get('title') as string;
    const folder = formData.get('folder') as string;

    if (!file || !title || !folder) {
      return c.json({ error: 'Missing required fields' }, 400);
    }

    const id = crypto.randomUUID();
    const ext = file.name.split('.').pop() || 'pdf';
    const path = `papers/${id}.${ext}`;
    const createdAt = Math.floor(Date.now() / 1000);

    const arrayBuffer = await file.arrayBuffer();
    const uint8Array = new Uint8Array(arrayBuffer);
    let binary = '';
    for (let i = 0; i < uint8Array.byteLength; i++) {
      binary += String.fromCharCode(uint8Array[i]);
    }
    const base64Content = btoa(binary);

    const githubResponse = await fetch(
      `https://api.github.com/repos/${c.env.GITHUB_OWNER}/${c.env.GITHUB_REPO}/contents/${path}`,
      {
        method: 'PUT',
        headers: {
          Authorization: `token ${c.env.GITHUB_TOKEN}`,
          'Content-Type': 'application/json',
          'User-Agent': 'ChrnovaPapers-Worker',
        },
        body: JSON.stringify({
          message: `Upload ${title}`,
          content: base64Content,
        }),
      }
    );

    if (!githubResponse.ok) {
      const errBody = await githubResponse.text();
      return c.json({ error: `GitHub upload failed (${githubResponse.status})`, detail: errBody }, 500);
    }

    await c.env.DB.prepare(
      `INSERT INTO papers (id, title, folder, path, size, device_id, downloads, created_at)
       VALUES (?, ?, ?, ?, ?, ?, 0, ?)`
    )
      .bind(id, title, folder, path, file.size, deviceId, createdAt)
      .run();

    return c.json({ id, path }, 201);
  } catch (e) {
    return c.json({ error: `Upload failed: ${e}` }, 500);
  }
});

// Verify GitHub star
app.get('/verify-star', async (c) => {
  const username = c.req.query('username');
  if (!username) {
    return c.json({ error: 'Missing username parameter' }, 400);
  }

  try {
    const token = c.env.GITHUB_TOKEN;
    if (!token) {
      return c.json({ starred: false, error: 'GITHUB_TOKEN not configured' }, 500);
    }

    const targetRepo = `${c.env.GITHUB_OWNER}/${c.env.GITHUB_STAR_REPO}`;
    const response = await fetch(
      `https://api.github.com/users/${encodeURIComponent(username)}/starred?per_page=100`,
      {
        headers: {
          'User-Agent': 'ChrnovaPapers-Worker',
          'Accept': 'application/vnd.github.v3+json',
          'Authorization': `token ${token}`,
        },
      }
    );

    if (!response.ok) {
      const errBody = await response.text();
      return c.json({ starred: false, error: `GitHub API ${response.status}: ${errBody}` }, 502);
    }

    const starredRepos = await response.json<Array<{ full_name: string }>>();
    const hasStarred = starredRepos.some(
      (repo) => repo.full_name.toLowerCase() === targetRepo.toLowerCase()
    );

    return c.json({ starred: hasStarred, username });
  } catch (e) {
    return c.json({ starred: false, error: `Verification failed: ${e}` }, 500);
  }
});

// Delete paper (only by device_id match)
app.delete('/papers/:id', async (c) => {
  const id = c.req.param('id');
  const deviceId = c.req.header('X-Device-Id');

  if (!deviceId) {
    return c.json({ error: 'X-Device-Id header required' }, 400);
  }

  const result = await c.env.DB.prepare(
    'SELECT * FROM papers WHERE id = ? AND device_id = ?'
  ).bind(id, deviceId).first<Paper>();

  if (!result) {
    return c.json({ error: 'Paper not found or unauthorized' }, 404);
  }

  await c.env.DB.prepare('DELETE FROM papers WHERE id = ?').bind(id).run();
  return c.json({ message: 'Paper deleted' });
});

export default app;
