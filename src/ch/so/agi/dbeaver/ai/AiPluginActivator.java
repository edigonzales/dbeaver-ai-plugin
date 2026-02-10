package ch.so.agi.dbeaver.ai;

import ch.so.agi.dbeaver.ai.config.AiSettingsService;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public final class AiPluginActivator extends AbstractUIPlugin {
    public static final String PLUGIN_ID = "ch.so.agi.dbeaver.ai";

    private static AiPluginActivator plugin;

    private AiSettingsService settingsService;

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        settingsService = null;
        super.stop(context);
    }

    public static AiPluginActivator getDefault() {
        return plugin;
    }

    public synchronized AiSettingsService getSettingsService() {
        if (settingsService == null) {
            settingsService = new AiSettingsService();
        }
        return settingsService;
    }
}
