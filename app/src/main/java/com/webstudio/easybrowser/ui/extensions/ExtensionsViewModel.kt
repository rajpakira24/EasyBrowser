package com.webstudio.easybrowser.ui.extensions

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import com.webstudio.easybrowser.managers.RuntimeManager
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.WebExtension
import org.mozilla.geckoview.WebExtensionController
import java.io.IOException

/** UI model for an installed add-on. */
data class InstalledExtension(
    val extension: WebExtension,
    val id: String,
    val name: String,
    val description: String,
    val enabled: Boolean,
    val rating: Double,
    val reviewCount: Int,
    val optionsUrl: String?,
)

/** UI model for an add-on in the AMO store list. */
data class StoreExtension(
    val id: String,
    val slug: String,
    val name: String,
    val summary: String,
    val iconUrl: String?,
    val installUrl: String,
    val amoUrl: String,
    val rating: Double,
    val reviewCount: Int,
    val installed: Boolean,
)

/** Full add-on detail (AMO addon endpoint) shown on the detail screen. */
data class ExtensionDetail(
    val id: String,
    val name: String,
    val description: String,
    val author: String,
    val version: String,
    val lastUpdated: String,
    val releaseNotes: String,
    val homepage: String?,
    val amoUrl: String,
    val iconUrl: String?,
    val installUrl: String,
    val rating: Double,
    val reviewCount: Int,
    val installed: Boolean,
)

class ExtensionsViewModel(app: Application) : AndroidViewModel(app) {

    // The built-in cosmetic ad blocker is internal — never list it as user-managed.
    private val builtInAdBlockerId = "easy-adblocker@easybrowser.local"
    private val recommendedUrl =
        "https://addons.mozilla.org/api/v5/addons/search/" +
            "?app=android&type=extension&page_size=30&sort=recommended"

    private val mainHandler = Handler(Looper.getMainLooper())
    private val http = OkHttpClient()
    private var marketplaceCall: Call? = null

    private val runtime = RuntimeManager.getRuntime(app)
    private val controller: WebExtensionController = runtime.webExtensionController

    val installed = mutableStateListOf<InstalledExtension>()
    val recommended = mutableStateListOf<StoreExtension>()
    // Packaged add-on icons, decoded from GeckoView, keyed by extension id.
    val installedIcons = mutableStateMapOf<String, ImageBitmap>()
    var isLoadingStore by mutableStateOf(true)
        private set
    var storeError by mutableStateOf(false)
        private set
    var statusMessage by mutableStateOf<String?>(null)
        private set
    var detail by mutableStateOf<ExtensionDetail?>(null)
        private set
    var detailLoading by mutableStateOf(false)
        private set
    private var detailCall: Call? = null

    // Toolbar (browser) action of each installed add-on, reported by GeckoView once the
    // ActionDelegate is attached. Its presence is what makes an add-on "openable".
    val actions = mutableStateMapOf<String, WebExtension.Action>()

    // Add-on popup / options page currently shown in the full-screen dialog.
    var popupSession by mutableStateOf<GeckoSession?>(null)
        private set
    var popupTitle by mutableStateOf("")
        private set

    // URL an add-on asked to open in a new tab; the screen hands it to the browser.
    var pendingOpenUrl by mutableStateOf<String?>(null)
        private set

    init {
        controller.promptDelegate = object : WebExtensionController.PromptDelegate {
            override fun onInstallPromptRequest(
                extension: WebExtension,
                permissions: Array<String>,
                origins: Array<String>,
                dataCollectionPermissions: Array<String>,
            ): GeckoResult<WebExtension.PermissionPromptResponse> {
                return GeckoResult.fromValue(
                    WebExtension.PermissionPromptResponse(true, true, true)
                )
            }
        }
        refreshInstalled()
        loadRecommended()
    }

    fun refreshInstalled() {
        controller.list().accept({ list ->
            onMain {
                installed.clear()
                list?.forEach { ext ->
                    if (ext != null && ext.id != builtInAdBlockerId) {
                        installed.add(ext.toInstalled())
                        loadInstalledIcon(ext)
                        attachDelegates(ext)
                    }
                }
                markRecommendedInstalled()
            }
        }, {
            onMain { installed.clear() }
        })
    }

    /**
     * GeckoView only reports an add-on's toolbar action and popup to an [WebExtension.ActionDelegate],
     * and its options page / new tabs to a [WebExtension.TabDelegate]. Without them an installed
     * add-on has no UI at all, so there was nothing to open after installing it.
     */
    private fun attachDelegates(ext: WebExtension) {
        val id = ext.id ?: return
        ext.setActionDelegate(object : WebExtension.ActionDelegate {
            override fun onBrowserAction(
                extension: WebExtension,
                session: GeckoSession?,
                action: WebExtension.Action,
            ) {
                onMain { actions[id] = action }
            }

            override fun onTogglePopup(
                extension: WebExtension,
                action: WebExtension.Action,
            ): GeckoResult<GeckoSession>? = createPopupSession(extension)

            override fun onOpenPopup(
                extension: WebExtension,
                action: WebExtension.Action,
            ): GeckoResult<GeckoSession>? = createPopupSession(extension)
        })
        ext.tabDelegate = object : WebExtension.TabDelegate {
            override fun onNewTab(
                source: WebExtension,
                details: WebExtension.CreateTabDetails,
            ): GeckoResult<GeckoSession>? {
                val url = details.url
                if (!url.isNullOrBlank()) onMain { pendingOpenUrl = url }
                return GeckoResult.fromValue(null)
            }

            override fun onOpenOptionsPage(source: WebExtension) {
                val url = source.metaData?.optionsPageUrl?.takeIf { it.isNotEmpty() } ?: return
                onMain { showInPopup(source.metaData?.name ?: source.id.orEmpty(), url) }
            }
        }
    }

    private fun createPopupSession(extension: WebExtension): GeckoResult<GeckoSession> {
        val result = GeckoResult<GeckoSession>()
        onMain {
            closePopup()
            // GeckoView only loads the popup URI into this session; it has to be opened here.
            val session = GeckoSession()
            session.open(runtime)
            popupTitle = extension.metaData?.name ?: extension.id.orEmpty()
            popupSession = session
            result.complete(session)
        }
        return result
    }

    private fun showInPopup(title: String, url: String) {
        closePopup()
        val session = GeckoSession()
        session.open(runtime)
        popupTitle = title
        popupSession = session
        session.loadUri(url)
    }

    /** Open an installed add-on: its toolbar popup if it has one, otherwise its options page. */
    fun openExtension(item: InstalledExtension) {
        val action = actions[item.id]
        if (action != null) {
            // Opens the popup through the ActionDelegate, or fires the add-on's onClicked handler.
            action.click()
            return
        }
        val options = item.optionsUrl ?: return
        showInPopup(item.name, options)
    }

    fun closePopup() {
        popupSession?.let { session ->
            try {
                if (session.isOpen) session.close()
            } catch (ignored: Exception) {
            }
        }
        popupSession = null
    }

    fun consumePendingOpenUrl() {
        pendingOpenUrl = null
    }

    private fun loadInstalledIcon(ext: WebExtension) {
        val id = ext.id ?: return
        if (installedIcons.containsKey(id)) return
        val icon = ext.metaData?.icon ?: return
        icon.getBitmap(96).accept({ bitmap ->
            if (bitmap != null) onMain { installedIcons[id] = bitmap.asImageBitmap() }
        }, { })
    }

    /** Open the detail screen for an already-installed add-on (full data fetched from AMO). */
    fun openDetail(item: InstalledExtension) {
        openDetail(
            StoreExtension(
                id = item.id,
                slug = "",
                name = item.name,
                summary = item.description,
                iconUrl = null,
                installUrl = "",
                amoUrl = amoUrlOf(item.id),
                rating = item.rating,
                reviewCount = item.reviewCount,
                installed = true,
            )
        )
    }

    private fun WebExtension.toInstalled(): InstalledExtension {
        val meta = metaData
        return InstalledExtension(
            extension = this,
            id = id ?: "",
            name = meta?.name ?: id ?: "",
            description = meta?.description ?: meta?.version.orEmpty(),
            enabled = meta?.enabled ?: true,
            rating = meta?.averageRating ?: 0.0,
            reviewCount = meta?.reviewCount ?: 0,
            optionsUrl = meta?.optionsPageUrl?.takeIf { it.isNotEmpty() },
        )
    }

    fun loadRecommended() {
        isLoadingStore = true
        storeError = false
        marketplaceCall?.cancel()
        val request = Request.Builder()
            .url(recommendedUrl)
            .header("Accept", "application/json")
            .build()
        marketplaceCall = http.newCall(request).also { call ->
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (call.isCanceled()) return
                    onMain {
                        isLoadingStore = false
                        storeError = true
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    val body = response.body?.string().orEmpty()
                    val parsed = if (response.isSuccessful) parseStore(body) else emptyList()
                    onMain {
                        recommended.clear()
                        recommended.addAll(parsed)
                        markRecommendedInstalled()
                        isLoadingStore = false
                        storeError = !response.isSuccessful
                    }
                }
            })
        }
    }

    fun install(url: String) {
        if (url.isBlank()) return
        statusMessage = "Installing…"
        controller.install(url, WebExtensionController.INSTALLATION_METHOD_MANAGER).accept({
            onMain {
                statusMessage = "Extension installed"
                refreshInstalled()
            }
        }, {
            onMain { statusMessage = "Install failed" }
        })
    }

    fun setEnabled(item: InstalledExtension, enable: Boolean) {
        val result = if (enable) {
            controller.enable(item.extension, WebExtensionController.EnableSource.USER)
        } else {
            controller.disable(item.extension, WebExtensionController.EnableSource.USER)
        }
        result.accept({ onMain { refreshInstalled() } }, { onMain { } })
    }

    fun update(item: InstalledExtension) {
        statusMessage = "Checking for updates…"
        controller.update(item.extension).accept({ updated ->
            onMain {
                statusMessage = if (updated != null) {
                    "Updated ${item.name}"
                } else {
                    "${item.name} is up to date"
                }
                refreshInstalled()
            }
        }, {
            onMain { statusMessage = "Update failed" }
        })
    }

    fun uninstall(item: InstalledExtension) {
        controller.uninstall(item.extension).accept({
            onMain {
                statusMessage = "Extension removed"
                refreshInstalled()
            }
        }, { onMain { } })
    }

    fun consumeStatus() {
        statusMessage = null
    }

    private fun markRecommendedInstalled() {
        if (recommended.isEmpty()) return
        val installedIds = installed.map { it.id.lowercase() }.toHashSet()
        val installedNames = installed.map { it.name.lowercase() }.toHashSet()
        for (i in recommended.indices) {
            val item = recommended[i]
            val isInstalled = installedIds.contains(item.id.lowercase()) ||
                installedNames.contains(item.name.lowercase())
            if (isInstalled != item.installed) {
                recommended[i] = item.copy(installed = isInstalled)
            }
        }
    }

    private fun parseStore(json: String): List<StoreExtension> {
        if (json.isBlank()) return emptyList()
        val out = ArrayList<StoreExtension>()
        val results = JSONObject(json).optJSONArray("results") ?: return out
        for (i in 0 until results.length()) {
            val item = results.optJSONObject(i) ?: continue
            val slug = item.optString("slug")
            val installUrl = installUrlOf(item, slug)
            val name = localized(item, "name")
            if (name.isBlank() || installUrl.isBlank()) continue
            val ratings = item.optJSONObject("ratings")
            out.add(
                StoreExtension(
                    id = item.optString("guid"),
                    slug = slug,
                    name = name,
                    summary = localized(item, "summary"),
                    iconUrl = item.optString("icon_url").takeIf { it.isNotEmpty() },
                    installUrl = installUrl,
                    amoUrl = amoUrlOf(slug),
                    rating = ratings?.optDouble("average", 0.0) ?: 0.0,
                    reviewCount = ratings?.optInt("count", 0) ?: 0,
                    installed = false,
                )
            )
        }
        return out
    }

    private fun installUrlOf(item: JSONObject, slug: String): String {
        val version = item.optJSONObject("current_version")
        val files = version?.optJSONArray("files")
        if (files != null) {
            var fallback = ""
            for (i in 0 until files.length()) {
                val file = files.optJSONObject(i) ?: continue
                val url = file.optString("url")
                if (url.isEmpty()) continue
                if (fallback.isEmpty()) fallback = url
                val platform = file.optString("platform")
                if (platform.equals("android", true) || platform.equals("all", true)) return url
            }
            if (fallback.isNotEmpty()) return fallback
        }
        return if (slug.isBlank()) {
            ""
        } else {
            "https://addons.mozilla.org/android/downloads/latest/$slug/latest.xpi"
        }
    }

    private fun localized(item: JSONObject, key: String): String {
        val value = item.opt(key) ?: return ""
        if (value is String) return value
        if (value is JSONObject) {
            value.optString("en-US").takeIf { it.isNotEmpty() }?.let { return it }
            val keys = value.keys()
            if (keys.hasNext()) return value.optString(keys.next())
        }
        return ""
    }

    fun openDetail(store: StoreExtension) {
        // Show immediately with the list data, then fill in the full description/metadata.
        detail = ExtensionDetail(
            id = store.id,
            name = store.name,
            description = store.summary,
            author = "",
            version = "",
            lastUpdated = "",
            releaseNotes = "",
            homepage = null,
            amoUrl = store.amoUrl,
            iconUrl = store.iconUrl,
            installUrl = store.installUrl,
            rating = store.rating,
            reviewCount = store.reviewCount,
            installed = store.installed,
        )
        detailLoading = true
        fetchDetail(store.id.ifEmpty { store.slug }, store)
    }

    fun closeDetail() {
        detailCall?.cancel()
        detail = null
        detailLoading = false
    }

    private fun fetchDetail(idOrSlug: String, store: StoreExtension) {
        if (idOrSlug.isBlank()) {
            detailLoading = false
            return
        }
        val encoded = try {
            java.net.URLEncoder.encode(idOrSlug, "UTF-8")
        } catch (e: Exception) {
            idOrSlug
        }
        val url = "https://addons.mozilla.org/api/v5/addons/addon/$encoded/?lang=en-US"
        detailCall?.cancel()
        val request = Request.Builder().url(url).header("Accept", "application/json").build()
        detailCall = http.newCall(request).also { call ->
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (call.isCanceled()) return
                    onMain { detailLoading = false }
                }

                override fun onResponse(call: Call, response: Response) {
                    val body = response.body?.string().orEmpty()
                    val parsed = if (response.isSuccessful) parseDetail(body, store) else null
                    onMain {
                        if (parsed != null) detail = parsed
                        detailLoading = false
                    }
                }
            })
        }
    }

    private fun parseDetail(json: String, store: StoreExtension): ExtensionDetail? {
        if (json.isBlank()) return null
        val root = JSONObject(json)
        val version = root.optJSONObject("current_version")
        val description = stripHtml(localized(root, "description"))
            .ifBlank { stripHtml(localized(root, "summary")) }
            .ifBlank { store.summary }
        return ExtensionDetail(
            id = root.optString("guid", store.id),
            name = localized(root, "name").ifBlank { store.name },
            description = description,
            author = firstAuthor(root),
            version = version?.optString("version").orEmpty(),
            lastUpdated = root.optString("last_updated").take(10),
            releaseNotes = version?.let { stripHtml(localized(it, "release_notes")) }.orEmpty(),
            homepage = homepageUrl(root),
            amoUrl = root.optString("url").ifBlank { store.amoUrl },
            iconUrl = root.optString("icon_url").takeIf { it.isNotEmpty() } ?: store.iconUrl,
            installUrl = installUrlOf(root, root.optString("slug", store.slug)),
            rating = root.optJSONObject("ratings")?.optDouble("average", store.rating) ?: store.rating,
            reviewCount = root.optJSONObject("ratings")?.optInt("count", store.reviewCount)
                ?: store.reviewCount,
            installed = store.installed,
        )
    }

    private fun firstAuthor(root: JSONObject): String {
        val authors = root.optJSONArray("authors") ?: return ""
        val first = authors.optJSONObject(0) ?: return ""
        return first.optString("name")
    }

    private fun homepageUrl(root: JSONObject): String? {
        val home = root.opt("homepage")
        if (home is String) return home.takeIf { it.isNotEmpty() }
        if (home is JSONObject) {
            val urlObj = home.optJSONObject("url")
            val url = urlObj?.optString("en-US")?.takeIf { it.isNotEmpty() }
                ?: home.optString("url").takeIf { it.isNotEmpty() }
            return url
        }
        return null
    }

    private fun amoUrlOf(slug: String): String =
        if (slug.isBlank()) "https://addons.mozilla.org/android/"
        else "https://addons.mozilla.org/firefox/addon/$slug/"

    private fun stripHtml(html: String): String {
        if (html.isBlank()) return ""
        return html
            .replace(Regex("(?i)<br\\s*/?>"), "\n")
            .replace(Regex("(?i)</p>"), "\n\n")
            .replace(Regex("<[^>]+>"), "")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }

    private fun onMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else mainHandler.post(block)
    }

    override fun onCleared() {
        closePopup()
        marketplaceCall?.cancel()
        detailCall?.cancel()
        super.onCleared()
    }
}
