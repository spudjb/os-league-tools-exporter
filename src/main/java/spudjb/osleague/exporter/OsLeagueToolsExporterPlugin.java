package spudjb.osleague.exporter;

import com.google.gson.Gson;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

@Slf4j
@PluginDescriptor(
	name = "OS League Tools Exporter"
)
public class OsLeagueToolsExporterPlugin extends Plugin
{
	@Inject
	private Client client;
	@Inject
	private ClientThread clientThread;
	@Inject
	private ClientToolbar clientToolbar;
	private OsLeagueToolsExporterPanel panel;
	private NavigationButton navButton;

	@Override
	protected void startUp() throws Exception
	{
		panel = new OsLeagueToolsExporterPanel(this);

		BufferedImage icon = ImageUtil.getResourceStreamFromClass(getClass(), "icon.png");

		navButton = NavigationButton.builder()
			.tooltip("Leagues Exporter")
			.icon(icon)
			.priority(6)
			.panel(panel)
			.build();

		clientToolbar.addNavigation(navButton);
	}

	@Override
	protected void shutDown() throws Exception
	{
		clientToolbar.removeNavigation(navButton);
	}

	private String doExportCSV(List<TaskState> taskStates) throws IOException
	{
		StringWriter sw = new StringWriter();
		CSVPrinter csvPrinter = new CSVPrinter(sw, CSVFormat.DEFAULT
			.withHeader("Name", "Completed"));

		for (TaskState taskState : taskStates)
		{
			csvPrinter.printRecord(taskState.getName(), taskState.isCompleted());
		}

		csvPrinter.flush();

		return sw.toString();
	}

	private static String cleanKey(String key) {
		return key.toLowerCase().replaceAll("[^a-zA-Z0-9 ]", "");
	}

	private String doExportTools(List<TaskState> taskStates) throws IOException, ExportException
	{
		OsToolsTaskData taskData;
		try (InputStream is = getClass().getResourceAsStream("taskData.json"))
		{
			taskData = new Gson().fromJson(new InputStreamReader(is), OsToolsTaskData.class);
		}

		Map<String, String> nameToId = new HashMap<>();
		for (OsToolsTask task : taskData.getTasks())
		{
			String key = cleanKey(task.getName());
			if(nameToId.containsKey(key)) {
				throw new ExportException("Duplicate key: " + key);
			}

			nameToId.put(key, task.getId());
		}

		List<String> strs = new ArrayList<>();
		for (TaskState taskState : taskStates)
		{
			if (taskState.isCompleted())
			{
				String key = cleanKey(taskState.getName());
				String value = nameToId.get(key);
				if (value == null)
				{
					throw new ExportException("Could not find ostools task id for task: " + key);
				}

				strs.add(value);
			}
		}

		String template = "(function(e) {\n" +
			"  let orig = JSON.parse(localStorage.tasks);\n" +
			"  if(orig.version != 3) throw new Error(\"Wrong version: \" + orig.version);\n" +
			"  orig.tasks = e;\n" +
			"  localStorage.tasks = JSON.stringify(orig);\n" +
			"  window.location.reload();\n" +
			"})(%s);";

		String json = new Gson().toJson(strs);

		return String.format(template, json);
	}

	private void doExport(ExportType exportType) throws Exception
	{
		GameState gameState = client.getGameState();
		if (gameState != GameState.LOGGED_IN)
		{
			throw new ExportException("Must be logged in");
		}

		Widget widget = client.getWidget(657, 10);
		if (widget == null || widget.isHidden())
		{
			throw new ExportException("Could not find leagues interface");
		}

		Widget[] taskWidgets = widget.getDynamicChildren();
		if (taskWidgets == null || taskWidgets.length == 0)
		{
			throw new ExportException("Leagues interface not loaded correctly");
		}


		List<TaskState> taskStates = new ArrayList<>(taskWidgets.length);
		for (Widget taskWidget : taskWidgets)
		{
			boolean completed = taskWidget.getTextColor() == 0xdc10d;
			taskStates.add(new TaskState(Text.removeTags(taskWidget.getText()), completed));
		}

		String str;
		switch (exportType)
		{
			case CSV:
				str = doExportCSV(taskStates);
				break;
			case OSTOOLS:
				str = doExportTools(taskStates);
				break;
			default:
				throw new ExportException("Unknown exportType? " + exportType);
		}

		SwingUtilities.invokeLater(() -> {
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			StringSelection selection = new StringSelection(str);
			clipboard.setContents(selection, selection);

			JOptionPane.showMessageDialog(client.getCanvas(), "Task list copied to clipboard!", "Leagues Export", JOptionPane.PLAIN_MESSAGE);
		});
	}

	public void export(ExportType exportType)
	{
		panel.setEnabled(false);

		clientThread.invoke(() -> {
			try
			{
				doExport(exportType);
			}
			catch (Exception e)
			{
				log.error("Could not perform export {}", exportType, e);
				SwingUtilities.invokeLater(() -> {
					JOptionPane.showMessageDialog(client.getCanvas(), e.getMessage(), "Export error", JOptionPane.ERROR_MESSAGE);
				});
			}
			finally
			{
				panel.setEnabled(true);
			}
		});
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class TaskState
	{
		private String name;
		private boolean completed;
	}

	@Data
	public static class OsToolsTaskData
	{
		private List<OsToolsTask> tasks;
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class OsToolsTask
	{
		private String id;
		private String name;
	}
}
