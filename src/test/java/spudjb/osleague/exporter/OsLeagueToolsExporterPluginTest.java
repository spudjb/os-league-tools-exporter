package spudjb.osleague.exporter;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class OsLeagueToolsExporterPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(OsLeagueToolsExporterPlugin.class);
		RuneLite.main(args);
	}
}