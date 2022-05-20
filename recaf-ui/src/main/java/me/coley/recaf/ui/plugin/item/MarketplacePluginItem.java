package me.coley.recaf.ui.plugin.item;

import dev.xdark.recaf.plugin.Plugin;
import dev.xdark.recaf.plugin.PluginContainer;
import dev.xdark.recaf.plugin.RecafPlugin;
import me.coley.recaf.config.Configs;
import me.coley.recaf.ui.plugin.DownloadFailedException;
import me.coley.recaf.ui.util.HttpUtil;
import me.coley.recaf.util.Directories;
import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

import static java.net.HttpURLConnection.HTTP_OK;

/**
 * Remote plugin information, used for UI display
 *
 * @author xtherk
 */
public class MarketplacePluginItem extends PluginItem {

    private static final Logger logger = Logging.get(MarketplacePluginItem.class);

    /**
     * @param uri         The path where the plugin is located
     * @param name        name of the plugin.
     * @param version     plugin version.
     * @param author      author of the plugin.
     * @param description plugin description.
     */
    public MarketplacePluginItem(URI uri, String name, String version, String author, String description) {
        super(uri, name, version, author, description);
    }

    /**
     * When the item is installed, the method should be call to refresh the status
     * @return Whether the plugin has been installed
     */
    @Override
    public boolean isInstalled() {
        return RecafPlugin.getInstance().getPlugin(name).isPresent();
    }


    /**
     * Install the plugin to local
     */
    public void install() {
        download().ifPresent(path -> {
            // load plugin
            Optional<PluginContainer<Plugin>> container = RecafPlugin.getInstance().loadPlugin(path);
            container.ifPresent(pc -> {
                // Enable after the default installation
                pc.getLoader().enablePlugin(pc);
                // Configuration persistence
                Configs.plugin().setEnabled(pc.getInformation(), true);
            });
            // refresh InstalledTab
            // todo: When the remote plugin is installed to the local, you need to refresh InstalledTab
        });
    }

    /**
     * Download the plugin
     *
     * @return Plugin path
     */
    private Optional<Path> download() {
        if (null == uri) {
            logger.info("Download links without existence");
            return Optional.empty();
        }
        try {
            HttpResponse<byte[]> response = HttpUtil.download(uri);
            if (HTTP_OK == response.statusCode()) {
                byte[] bytes = response.body();
                Path pluginPath = Directories.getPluginDirectory().resolve(String.format("%s-%s.jar", name, version));
                Files.write(pluginPath, bytes, StandardOpenOption.CREATE_NEW);
                return Optional.of(pluginPath);
            }
        } catch (IOException | InterruptedException e) {
            throw new DownloadFailedException("Download plugin failed!", e);
        }
        return Optional.empty();
    }
}
