package spudjb.osleague.exporter;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.PluginPanel;

public class OsLeagueToolsExporterPanel extends PluginPanel
{
	private final OsLeagueToolsExporterPlugin plugin;
	private final JPanel listContainer = new JPanel();
	private final JButton export = new JButton("osleague.tools export");
	private final JButton exportCsv = new JButton("CSV export");

	OsLeagueToolsExporterPanel(OsLeagueToolsExporterPlugin plugin) throws IOException
	{
		this.plugin = plugin;

		setBorder(null);
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		listContainer.setLayout(new BoxLayout(listContainer, BoxLayout.Y_AXIS));

		String howto;
		try (InputStream is = getClass().getResourceAsStream("howto.txt"))
		{
			howto = CharStreams.toString(new InputStreamReader(is, Charsets.UTF_8));
		}

		listContainer.add(new JLabel(howto));

		export.addActionListener(a -> {
			plugin.export(ExportType.OSTOOLS);
		});

		listContainer.add(export);

		exportCsv.addActionListener(a -> {
			plugin.export(ExportType.CSV);
		});

		listContainer.add(exportCsv);

		add(listContainer);
	}

	public void setEnabled(boolean enabled)
	{
		export.setEnabled(enabled);
		exportCsv.setEnabled(enabled);
	}
}
