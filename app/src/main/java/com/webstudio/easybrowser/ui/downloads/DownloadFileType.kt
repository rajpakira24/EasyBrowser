package com.webstudio.easybrowser.ui.downloads

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.webstudio.easybrowser.R

/**
 * A download's visual file-type category: a distinctive glyph plus a badge tint. Resolution
 * prefers the MIME type but falls back to the filename extension, so files that arrive with a
 * null or generic (`application/octet-stream`) MIME — e.g. `.apk`, `.zip`, `.docx` — still get
 * a meaningful icon instead of a blank sheet.
 */
enum class DownloadFileType(@DrawableRes val icon: Int, val tint: Color) {
    IMAGE(R.drawable.ic_image, Color(0xFF4DAA89)),
    VIDEO(R.drawable.ic_video, Color(0xFFE05B45)),
    AUDIO(R.drawable.ic_audio, Color(0xFF9787F3)),
    PDF(R.drawable.ic_pdf, Color(0xFFE5484D)),
    ARCHIVE(R.drawable.ic_archive, Color(0xFFE0A82E)),
    DOCUMENT(R.drawable.ic_file_document, Color(0xFF3B82B8)),
    SPREADSHEET(R.drawable.ic_file_spreadsheet, Color(0xFF2E9E5B)),
    PRESENTATION(R.drawable.ic_file_presentation, Color(0xFFE8833A)),
    APK(R.drawable.ic_file_apk, Color(0xFF3DDC84)),
    CODE(R.drawable.ic_file_code, Color(0xFF5C6BC0)),
    TEXT(R.drawable.ic_text, Color(0xFF6F7482)),
    OTHER(R.drawable.ic_file, Color(0xFF6F7482));

    companion object {
        fun resolve(mimeType: String?, fileName: String?): DownloadFileType {
            val mime = mimeType?.trim()?.lowercase().orEmpty()
            when {
                mime.startsWith("image/") -> return IMAGE
                mime.startsWith("video/") -> return VIDEO
                mime.startsWith("audio/") -> return AUDIO
                mime == "application/pdf" -> return PDF
                mime == "application/vnd.android.package-archive" -> return APK
                mime == "application/zip" ||
                    mime == "application/x-zip-compressed" ||
                    mime == "application/x-rar-compressed" ||
                    mime == "application/x-7z-compressed" ||
                    mime == "application/x-tar" ||
                    mime == "application/gzip" -> return ARCHIVE
                mime.startsWith("text/") -> return TEXT
            }
            val ext = fileName?.substringAfterLast('.', "")?.lowercase().orEmpty()
            return when (ext) {
                "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif",
                "svg", "avif", "ico" -> IMAGE
                "mp4", "mkv", "webm", "avi", "mov", "3gp", "flv", "wmv", "m4v" -> VIDEO
                "mp3", "wav", "ogg", "flac", "aac", "m4a", "opus", "wma" -> AUDIO
                "pdf" -> PDF
                "zip", "rar", "7z", "tar", "gz", "bz2", "xz", "tgz" -> ARCHIVE
                "doc", "docx", "odt", "rtf" -> DOCUMENT
                "xls", "xlsx", "ods", "csv" -> SPREADSHEET
                "ppt", "pptx", "odp" -> PRESENTATION
                "apk", "apks", "xapk", "aab" -> APK
                "js", "ts", "json", "html", "htm", "css", "xml", "java", "kt",
                "kts", "py", "c", "cpp", "h", "cs", "go", "rb", "php", "sh",
                "yml", "yaml", "gradle", "sql" -> CODE
                "txt", "md", "log" -> TEXT
                else -> OTHER
            }
        }
    }
}
