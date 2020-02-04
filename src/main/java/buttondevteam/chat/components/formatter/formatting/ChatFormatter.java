package buttondevteam.chat.components.formatter.formatting;

import buttondevteam.chat.commands.ucmds.admin.DebugCommand;
import buttondevteam.chat.components.formatter.ChatProcessing;
import buttondevteam.lib.architecture.ConfigData;
import buttondevteam.lib.architecture.IHaveConfig;
import buttondevteam.lib.chat.Color;
import buttondevteam.lib.chat.Priority;
import lombok.Builder;
import lombok.Data;
import lombok.val;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A {@link ChatFormatter} shows what formatting to use based on regular expressions. {@link ChatFormatter#Combine(List, String, TellrawPart, IHaveConfig)} is used to turn it into a {@link TellrawPart}, combining
 * intersecting parts found, for example when {@code _abc*def*ghi_} is said in chat, it'll turn it into an underlined part, then an underlined <i>and italics</i> part, finally an underlined part
 * again.
 *
 * @author NorbiPeti
 */
@SuppressWarnings("UnusedAssignment")
@Data
@Builder
public final class ChatFormatter {
	Pattern regex;
	boolean italic;
	boolean bold;
	boolean underlined;
	boolean strikethrough;
	boolean obfuscated;
	Color color;
	TriFunc<String, ChatFormatter, FormattedSection, String> onmatch;
	String openlink;
	@Builder.Default
	Priority priority = Priority.Normal;
	@Builder.Default
	short removeCharCount = 0;
	@Builder.Default
	Type type = Type.Normal;
	String hoverText;
	String name;

	@Override
	public String toString() {
		return "ChatFormatter{" +
			"name='" + name + '\'' +
			'}';
	}

	public static ChatFormatterBuilder builder(String name, Pattern regex) {
		return builder().regex(regex).name(name);
	}

	private static ChatFormatterBuilder builder() {
		return new ChatFormatterBuilder();
	}

	private ConfigData<Boolean> enabled(IHaveConfig config) {
		return config.getData(name + ".enabled", true);
	}

	public enum Type {
		Normal,
		/**
		 * Matches a start and an end section which gets converted to one section (for example see italics)
		 */
		Range,
		/**
		 * Exclude matching area from further processing (besides this formatter)
		 */
		Excluder
	}

	@FunctionalInterface
	public interface TriFunc<T1, T2, T3, R> {
		R apply(T1 x1, T2 x2, T3 x3);
	}

	public static void Combine(List<ChatFormatter> formatters, String str, TellrawPart tp, IHaveConfig config) {
		/*
		 * This method assumes that there is always a global formatter
		 */
		header("ChatFormatter.Combine begin");
		ArrayList<FormattedSection> sections = new ArrayList<>();

		if (config != null) //null if testing
			formatters.removeIf(cf -> !cf.enabled(config).get()); //Remove disabled formatters
		createSections(formatters, str, sections, true);

		header("Section creation (excluders done)");
		createSections(formatters, str, sections, false);
		sortSections(sections);

		/*
		 * 0: Start - 1: End index
		 */
		val remchars = new ArrayList<int[]>();

		header("Range section conversion");
		sections = convertRangeSections(str, sections, remchars);

		header("Adding remove chars (RC)"); // Important to add after the range section conversion
		addRemChars(sections, remchars, str);

		header("Section combining");
		combineSections(str, sections);

		header("Section applying");
		applySections(str, tp, sections, remchars);
		header("ChatFormatter.Combine done");
	}

	private static void createSections(List<ChatFormatter> formatters, String str, ArrayList<FormattedSection> sections,
	                                   boolean excluders) {
		for (ChatFormatter formatter : formatters) {
			if (excluders == (formatter.type != Type.Excluder))
				continue; //If we're looking at excluders and this isn't one, skip - or vica-versa
			Matcher matcher = formatter.regex.matcher(str);
			while (matcher.find()) {
				DebugCommand.SendDebugMessage("Found match from " + matcher.start() + " to " + (matcher.end() - 1));
				DebugCommand.SendDebugMessage("With " + (excluders ? "excluder " : "") + "formatter: " + formatter);
				sendMessageWithPointer(str, matcher.start(), matcher.end() - 1);
				if (formatter.regex != ChatProcessing.ENTIRE_MESSAGE_PATTERN && sections.stream().anyMatch(fs -> fs.type == Type.Excluder && (fs.End >= matcher.start() && fs.Start <= matcher.end() - 1))) {
					DebugCommand.SendDebugMessage("Ignoring formatter because of an excluder");
					continue; //Exclude areas matched by excluders - Range sections are correctly handled afterwards
				}
				ArrayList<String> groups = new ArrayList<>();
				for (int i = 0; i < matcher.groupCount(); i++)
					groups.add(matcher.group(i + 1));
				if (groups.size() > 0)
					DebugCommand.SendDebugMessage("First group: " + groups.get(0));
				FormattedSection section = new FormattedSection(formatter, matcher.start(), matcher.end() - 1, groups,
					formatter.type);
				sections.add(section);
			}
		}
	}

	private static void newCombine(String str, ArrayList<FormattedSection> sections, ArrayList<int[]> remchars) {
		var stack = new Stack<FormattedSection>();
		for (int i = 0; i < str.length(); i++) {
			for (Iterator<FormattedSection> iterator = sections.iterator(); iterator.hasNext(); ) {
				FormattedSection section = iterator.next();
				if (section.Start <= i) {
					stack.push(section);
					iterator.remove();
				}
			}
		}
	}

	private static ArrayList<FormattedSection> convertRangeSections(String str, ArrayList<FormattedSection> sections, ArrayList<int[]> remchars) {
		ArrayList<FormattedSection> combined = new ArrayList<>();
		Map<ChatFormatter, FormattedSection> nextSection = new HashMap<>();
		boolean escaped = false;
		int takenStart = -1, takenEnd = -1;
		ChatFormatter takenFormatter = null;
		boolean takenByBigGuy = false; //Can't win against him (finished sections take precedence)
		for (final FormattedSection section : sections) {
			// Set ending to -1 until closed with another 1 long "section" - only do this if IsRange is true
			if (section.type != Type.Range) {
				escaped = section.Formatters.contains(ChatProcessing.ESCAPE_FORMATTER) && !escaped; // Enable escaping on first \, disable on second
				if (escaped) {// Don't add the escape character
					remchars.add(new int[]{section.Start, section.Start});
					DebugCommand.SendDebugMessage("Found escaper section: " + section);
				} else {
					combined.add(section); // The above will delete the \
					DebugCommand.SendDebugMessage("Added section: " + section);
				}
				sendMessageWithPointer(str, section.Start, section.End);
				continue;
			}
			if (!escaped) {
				ChatFormatter formatter = section.Formatters.get(0);
				if (section.Start == takenStart || (section.Start > takenStart && section.Start < takenEnd)) {
					/*
					 * if (nextSection.containsKey(section.Formatters.get(0)) ? section.RemCharFromStart <= takenEnd - takenStart : section.RemCharFromStart > takenEnd - takenStart) {
					 */
					if (takenByBigGuy || formatter.removeCharCount < takenEnd - takenStart) {
						DebugCommand.SendDebugMessage("Lose: " + section);
						sendMessageWithPointer(str, section.Start, section.End);
						DebugCommand.SendDebugMessage("And win: " + takenFormatter);
						continue; // The current section loses
					}
					nextSection.remove(takenFormatter); // The current section wins
					DebugCommand.SendDebugMessage("Win: " + section);
					sendMessageWithPointer(str, section.Start, section.End);
					DebugCommand.SendDebugMessage("And lose: " + takenFormatter);
				}
				boolean hasFormatter = nextSection.containsKey(formatter);
				if (!hasFormatter) {
					val ff = formatter;
					val cfo = nextSection.keySet().stream().filter(f -> f.removeCharCount > ff.removeCharCount).findAny();
					if (cfo.isPresent()) {
						//formatter = cfo.get();
						val s = nextSection.get(cfo.get());
						int takenS = section.Start, takenE = section.Start + formatter.removeCharCount;
						if (s.Start == takenS || (s.Start > takenS && s.Start < takenE)) { //Peek()
							hasFormatter = true;
							continue; //Not the formatter we're looking for - TODO: It doesn't fix the problem of italics at the end
						}
					}
				}
				takenStart = section.Start;
				takenEnd = section.Start + formatter.removeCharCount;
				takenFormatter = formatter;
				if (hasFormatter) {
					FormattedSection s = nextSection.remove(formatter);
					//HACK? If we can find another section that removes more characters, finish that instead
					// section: the ending marker section - s: the to-be full section
					s.End = takenEnd - 1; //Take the remCharCount into account as well
					// s.IsRange = false; // IsRange means it's a 1 long section indicating a start or an end
					combined.add(s);
					takenByBigGuy = true;
					DebugCommand.SendDebugMessage("Finished section: " + s);
					sendMessageWithPointer(str, s.Start, s.End);
				} else {
					DebugCommand.SendDebugMessage("Adding next section: " + section);
					sendMessageWithPointer(str, section.Start, section.End);
					nextSection.put(formatter, section);
					takenByBigGuy = false;
				}
				DebugCommand
					.SendDebugMessage("New area taken: (" + takenStart + "-" + takenEnd + ") " + takenFormatter);
				sendMessageWithPointer(str, takenStart, takenEnd);
			} else {
				DebugCommand.SendDebugMessage("Skipping section: " + section); // This will keep the text (character)
				sendMessageWithPointer(str, section.Start, section.End);
				escaped = false; // Reset escaping if applied, like if we're at the '*' in '\*'
			}
		}
		//Do not finish unfinished sections, ignore them
		sections = combined;
		return sections;
	}

	private static void addRemChars(ArrayList<FormattedSection> sections, ArrayList<int[]> remchars, String str) {
		sections.stream()
			.flatMap(fs -> fs.Formatters.stream().filter(cf -> cf.removeCharCount > 0)
				.mapToInt(cf -> cf.removeCharCount).mapToObj(rcc -> new int[]{fs.Start, fs.Start + rcc - 1}))
			.forEach(remchars::add);
		sections.stream()
			.flatMap(fs -> fs.Formatters.stream().filter(cf -> cf.removeCharCount > 0)
				.mapToInt(cf -> cf.removeCharCount).mapToObj(rcc -> new int[]{fs.End - rcc + 1, fs.End}))
			.forEach(remchars::add);
		DebugCommand.SendDebugMessage("Added remchars:");
		DebugCommand.SendDebugMessage(remchars.stream().map(Arrays::toString).collect(Collectors.joining("; ")));
		sendMessageWithPointer(str,
			remchars.stream().flatMapToInt(Arrays::stream).toArray());
	}

	private static void combineSections(String str, ArrayList<FormattedSection> sections) {
		for (int i = 1; i < sections.size(); i++) {
			DebugCommand.SendDebugMessage("i: " + i);
			final FormattedSection firstSection;
			final FormattedSection lastSection;
			{
				FormattedSection firstSect = sections.get(i - 1);
				FormattedSection lastSect = sections.get(i);
				if (firstSect.Start > lastSect.Start) { //The first can't start later
					var section = firstSect;
					firstSect = lastSect;
					lastSect = section;
				}
				firstSection = firstSect;
				lastSection = lastSect;
			}
			DebugCommand.SendDebugMessage("Combining sections " + firstSection);
			sendMessageWithPointer(str, firstSection.Start, firstSection.End);
			DebugCommand.SendDebugMessage(" and " + lastSection);
			sendMessageWithPointer(str, lastSection.Start, lastSection.End);
			if (firstSection.Start == lastSection.Start && firstSection.End == lastSection.End) {
				firstSection.Formatters.addAll(lastSection.Formatters);
				firstSection.Matches.addAll(lastSection.Matches);
				firstSection.type = lastSection.type;
				DebugCommand.SendDebugMessage("To section " + firstSection);
				sendMessageWithPointer(str, firstSection.Start, firstSection.End);
				sections.remove(i);
				i = 0;
				sortSections(sections);
				continue;
			} else if (firstSection.End > lastSection.Start && firstSection.Start < lastSection.End) {
				int origend2 = firstSection.End;
				firstSection.End = lastSection.Start - 1;
				int origend = lastSection.End;
				FormattedSection section = new FormattedSection(firstSection.Formatters, lastSection.Start, origend,
					firstSection.Matches, Type.Normal);
				section.Formatters.addAll(lastSection.Formatters);
				section.Matches.addAll(lastSection.Matches); // TODO: Clean
				sections.add(i, section);
				// Use the properties of the first section not the second one
				lastSection.Formatters.clear();
				lastSection.Formatters.addAll(firstSection.Formatters);
				lastSection.Matches.clear();
				lastSection.Matches.addAll(firstSection.Matches);

				lastSection.Start = origend + 1;
				lastSection.End = origend2;

				Predicate<FormattedSection> removeIfNeeded = s -> {
					if (s.Start < 0 || s.End < 0 || s.Start > s.End) {
						DebugCommand.SendDebugMessage("  Removed: " + s);
						sendMessageWithPointer(str, s.Start, s.End);
						sections.remove(s);
						return true;
					}
					return false;
				};

				DebugCommand.SendDebugMessage("To sections");
				if (!removeIfNeeded.test(firstSection)) {
					DebugCommand.SendDebugMessage("  1:" + firstSection + "");
					sendMessageWithPointer(str, firstSection.Start, firstSection.End);
				}
				if (!removeIfNeeded.test(section)) {
					DebugCommand.SendDebugMessage("  2:" + section + "");
					sendMessageWithPointer(str, section.Start, section.End);
				}
				if (!removeIfNeeded.test(lastSection)) {
					DebugCommand.SendDebugMessage("  3:" + lastSection);
					sendMessageWithPointer(str, lastSection.Start, lastSection.End);
				}
				i = 0;
			}
			sortSections(sections);
			if (i == 0) continue;
			for (int j = i - 1; j <= i + 1; j++) {
				if (j < sections.size() && sections.get(j).End < sections.get(j).Start) {
					DebugCommand.SendDebugMessage("Removing section: " + sections.get(j));
					sendMessageWithPointer(str, sections.get(j).Start, sections.get(j).End);
					sections.remove(j);
					j--;
					i = 0;
				}
			}
		}
	}

	private static void applySections(String str, TellrawPart tp, ArrayList<FormattedSection> sections, ArrayList<int[]> remchars) {
		TellrawPart lasttp = null;
		String lastlink = null;
		for (FormattedSection section : sections) {
			DebugCommand.SendDebugMessage("Applying section: " + section);
			String originaltext;
			int start = section.Start, end = section.End;
			DebugCommand.SendDebugMessage("Start: " + start + " - End: " + end);
			sendMessageWithPointer(str, start, end);
			/*DebugCommand.SendDebugMessage("RCS: "+remchars.stream().filter(rc -> rc[0] <= start && start <= rc[1]).count());
			DebugCommand.SendDebugMessage("RCE: "+remchars.stream().filter(rc -> rc[0] <= end && end <= rc[1]).count());
			DebugCommand.SendDebugMessage("RCI: "+remchars.stream().filter(rc -> start < rc[0] || rc[1] < end).count());*/
			val rci = remchars.stream().filter(rc -> (rc[0] <= start && rc[1] >= start)
				|| (rc[0] >= start && rc[1] <= end)
				|| (rc[0] <= end && rc[1] >= end)).sorted(Comparator.comparingInt(rc -> rc[0] * 10000 + rc[1])).toArray(int[][]::new);
			/*if (rcs.isPresent())
				s = rcs.get()[1] + 1;
			if (rce.isPresent())
				e = rce.get()[0] - 1;
			DebugCommand.SendDebugMessage("After RC - Start: " + s + " - End: " + e);
			if (e - s < 0) { //e-s==0 means the end char is the same as start char, so one char message
				DebugCommand.SendDebugMessage("Skipping section because of remchars (length would be " + (e - s + 1) + ")");
				continue;
			}*/
			DebugCommand.SendDebugMessage("Applying RC: " + Arrays.stream(rci).map(Arrays::toString).collect(Collectors.joining(", ", "[", "]")));
			originaltext = str.substring(start, end + 1);
			val sb = new StringBuilder(originaltext);
			for (int x = rci.length - 1; x >= 0; x--)
				sb.delete(Math.max(rci[x][0] - start, 0), Math.min(rci[x][1] - start, end) + 1); //Delete going backwards
			originaltext = sb.toString();
			if (originaltext.length() == 0) {
				DebugCommand.SendDebugMessage("Skipping section because of remchars");
				continue;
			}
			DebugCommand.SendDebugMessage("Section text: " + originaltext);
			String openlink = null;
			section.Formatters.sort(Comparator.comparing(cf2 -> cf2.priority.GetValue())); //Apply the highest last, to overwrite previous ones
			TellrawPart newtp = new TellrawPart("");
			for (ChatFormatter formatter : section.Formatters) {
				DebugCommand.SendDebugMessage("Applying formatter: " + formatter);
				if (formatter.onmatch != null)
					originaltext = formatter.onmatch.apply(originaltext, formatter, section);
				if (formatter.color != null)
					newtp.setColor(formatter.color);
				if (formatter.bold)
					newtp.setBold(true);
				if (formatter.italic)
					newtp.setItalic(true);
				if (formatter.underlined)
					newtp.setUnderlined(true);
				if (formatter.strikethrough)
					newtp.setStrikethrough(true);
				if (formatter.obfuscated)
					newtp.setObfuscated(true);
				if (formatter.openlink != null)
					openlink = formatter.openlink;
				if (formatter.hoverText != null)
					newtp.setHoverEvent(TellrawEvent.create(TellrawEvent.HoverAction.SHOW_TEXT, formatter.hoverText));
			}
			if (lasttp != null && newtp.getColor() == lasttp.getColor()
				&& newtp.isBold() == lasttp.isBold()
				&& newtp.isItalic() == lasttp.isItalic()
				&& newtp.isUnderlined() == lasttp.isUnderlined()
				&& newtp.isStrikethrough() == lasttp.isStrikethrough()
				&& newtp.isObfuscated() == lasttp.isObfuscated()
				&& Objects.equals(openlink, lastlink)) {
				DebugCommand.SendDebugMessage("This part has the same properties as the previous one, combining.");
				lasttp.setText(lasttp.getText() + originaltext);
				continue; //Combine parts with the same properties
			}
			lastlink = openlink;
			newtp.setText(originaltext);
			if (openlink != null && openlink.length() > 0) {
				newtp.setClickEvent(TellrawEvent.create(TellrawEvent.ClickAction.OPEN_URL,
					(section.Matches.size() > 0 ? openlink.replace("$1", section.Matches.get(0)) : openlink)))
					.setHoverEvent(TellrawEvent.create(TellrawEvent.HoverAction.SHOW_TEXT,
						new TellrawPart("Click to open").setColor(Color.Blue)));
			}
			tp.addExtra(newtp);
			lasttp = newtp;
		}
	}

	private static void sortSections(ArrayList<FormattedSection> sections) {
		sections.sort(
			(s1, s2) -> s1.Start == s2.Start
				? s1.End == s2.End ? Integer.compare(s2.Formatters.get(0).priority.GetValue(),
				s1.Formatters.get(0).priority.GetValue()) : Integer.compare(s1.End, s2.End)
				: Integer.compare(s1.Start, s2.Start));
	}

	private static void sendMessageWithPointer(String str, int... pointer) {
		DebugCommand.SendDebugMessage(str);
		StringBuilder sb = new StringBuilder(str.length());
		Arrays.sort(pointer);
		for (int i = 0; i < pointer.length; i++) {
			for (int j = 0; j < pointer[i] - (i > 0 ? pointer[i - 1] + 1 : 0); j++)
				sb.append(' ');
			if (pointer[i] == (i > 0 ? pointer[i - 1] : -1))
				continue;
			sb.append('^');
		}
		DebugCommand.SendDebugMessage(sb.toString());
	}

	private static void header(String message) {
		DebugCommand.SendDebugMessage("\n--------\n" + message + "\n--------\n");
	}
}