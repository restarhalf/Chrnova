package restarhalf.stellar.schedule.ui.image

internal fun toAsyncImageModel(source: String): Any {
    if (source.startsWith("content://") || source.startsWith("file:/") || source.contains("://")) {
        return source
    }

    if (source.length >= 3 && source[1] == ':' && (source[2] == '\\' || source[2] == '/')) {
        return "file:///${source.replace('\\', '/')}"
    }

    if (source.startsWith("/")) {
        return "file://$source"
    }

    return source
}
