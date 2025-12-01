package net.envexus.svcmute.integrations.svcadmin;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;

import java.util.logging.Logger;

public class SVCPlugin implements VoicechatPlugin {
	public static final Logger LOGGER = Logger.getLogger(SVCPlugin.class.getName());
	private VoicechatServerApi voicechatApi;

	@Override
	public String getPluginId() {
		return "simplevoicechatadmin";
	}

	@Override
	public void initialize(VoicechatApi api) {
		if (api instanceof VoicechatServerApi) {
			this.voicechatApi = (VoicechatServerApi) api;
			LOGGER.info("SVCPlugin initialized.");
		} else {
			LOGGER.severe("SimpleVoiceChat API is not available.");
		}
	}

	public VoicechatServerApi getVoicechatApi() {
		return this.voicechatApi;
	}
}
