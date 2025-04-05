/*
  Metric SAE Sizes #5 - Metric Wrench Sizes Close To SAE Sizes
  Written by: Keith Fenske, http://kwfenske.github.io/
  Tuesday, 25 March 2025
  Java class name: MetricSaeSizes5
  Copyright (c) 2025 by Keith Fenske.  Apache License or GNU GPL.

  This is a Java 1.4 graphical (GUI) application to calculate how close metric
  bolt/nut/socket/wrench sizes are to fractional/inch/SAE/standard sizes.  The
  metric range is up to 250 mm (25 cm) in unit steps from 0.1 to 10 mm (1 cm),
  and the SAE range is up to 10 inches with steps from 1/256 to 1/2 inch.  Not
  surprisingly, the most interesting sizes are those of ordinary tools.

  Rounding towards the nearest measurement unit ignores an obvious physical
  reality: a wrench that is too small is more of a problem than a wrench that
  is too big.  A bias can be added so the rounding occurs from say 30% of a
  unit smaller to 70% bigger, instead of 50-50.  Explaining that, or any other
  technical details, defeats the point of a simple chart.  You would also need
  to justify the chosen bias.  The word "rounded" is associated with damage to
  bolt heads and nuts, so maybe use the word "nearest" in public documentation.

  Apache License or GNU General Public License
  --------------------------------------------
  MetricSaeSizes5 is free software and has been released under the terms and
  conditions of the Apache License (version 2.0 or later) and/or the GNU
  General Public License (GPL, version 2 or later).  This program is
  distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY,
  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
  PARTICULAR PURPOSE.  See the license(s) for more details.  You should have
  received a copy of the licenses along with this program.  If not, see the
  http://www.apache.org/licenses/ and http://www.gnu.org/licenses/ web pages.

  Graphical Versus Console Application
  ------------------------------------
  The Java command line may contain options for the position and size of the
  application window, and the size of the display font.  See the "-?" option
  for a help summary:

      java  MetricSaeSizes5  -?

  The command line has more options than are visible in the graphical
  interface.  An option such as -u16 or -u18 is recommended for the font size.

  Restrictions and Limitations
  ----------------------------
  The formatting of the output numbers is sensitive to the current locale; the
  parsing of the input numbers is not.
*/

import java.awt.*;                // older Java GUI support
import java.awt.event.*;          // older Java GUI event support
import java.io.*;                 // standard I/O
import java.text.*;               // number formatting
import java.util.regex.*;         // regular expressions
import javax.swing.*;             // newer Java GUI support
import javax.swing.border.*;      // decorative borders

public class MetricSaeSizes5
{
  /* constants */

  static final String COPYRIGHT_NOTICE =
    "Copyright (c) 2025 by Keith Fenske.  Apache License or GNU GPL.";
  static final int DEFAULT_HEIGHT = -1; // default window height in pixels
  static final int DEFAULT_LEFT = 50; // default window left position ("x")
  static final int DEFAULT_TOP = 50; // default window top position ("y")
  static final int DEFAULT_WIDTH = -1; // default window width in pixels
  static final int EXIT_FAILURE = -1; // incorrect request or errors found
//static final int EXIT_SUCCESS = 1; // request completed successfully
  static final int EXIT_UNKNOWN = 0; // don't know or nothing really done
  static final String[] FONT_SIZES = {"12", "14", "16", "18", "20", "24", "30",
    "36"};                        // available sizes for output text area
  static final String[] MET_UNIT_CHOICES = {"0.1", "0.2", "0.5", "1", "2", "5",
    "10"};
  static final String MET_UNIT_DEFAULT = "1";
  static final int MIN_FRAME = 200; // minimum window height or width in pixels
  static final double MM_PER_INCH = 25.4; // exact millimeters per inch
  static final String PROGRAM_TITLE =
    "Metric Wrench Sizes Close To SAE Sizes - by: Keith Fenske";
  static final String[] SAE_UNIT_CHOICES = {"1/256", "1/128", "1/64", "1/32",
    "1/16", "1/8", "1/4", "1/2"};
  static final String SAE_UNIT_DEFAULT = "1/16";
  static final String SYSTEM_FONT = "Dialog"; // this font is always available

  /* class variables */

  static JTextField biasDialog;   // bias when rounding to whole numbers
  static double biasValue;        // parsed bias value from user
  static JButton cancelButton;    // graphical button for <cancelFlag>
  static boolean cancelFlag;      // our signal from user to stop processing
  static Thread doStartThread;    // separate thread for doStartButton() method
  static JButton exitButton;      // "Exit" button for ending this application
  static JFileChooser fileChooser; // asks for input and output file names
  static JComboBox fontNameDialog; // graphical option for <outputFontName>
  static JComboBox fontSizeDialog; // graphical option for <outputFontSize>
  static NumberFormat formatPointOne, formatPointThree, formatPointFive;
                                  // format with decimal digits
  static JFrame mainFrame;        // this application's GUI window
  static JTextField metDialogFirst, metDialogLast; // metric first, last sizes
  static JComboBox metDialogUnits; // metric steps or units
  static double metValueFirst, metValueLast; // parsed metric numbers from user
  static boolean mswinFlag;       // true if running on Microsoft Windows
  static Font outputFont;         // font for output text area
  static String outputFontName;   // preferred font name for output text
  static int outputFontSize;      // normal font size or chosen by user
  static JTextArea outputText;    // generated report while processing
  static JTextField saeDialogFirst, saeDialogLast; // SAE first, last sizes
  static JComboBox saeDialogUnits; // SAE steps or units
  static double saeValueFirst, saeValueLast; // parsed SAE numbers from user
  static JButton saveButton;      // "Save" button for writing output text
  static JButton startButton;     // "Start" button to begin processing
  static int unitsPerInch;        // SAE is fractional using integers
  static double unitsPerMm;       // while metric uses floating point

/*
  main() method

  We run as a graphical application only.  Set the window layout and then let
  the graphical interface run the show.
*/
  public static void main(String[] args)
  {
    ActionListener action;        // our shared action listener
    Font commonFont;              // font for buttons, labels, status, etc
    String commonFontName;        // preferred font name for buttons, etc
    int commonFontSize;           // normal font size or chosen by user
    Border emptyBorder;           // remove borders around text areas
    int i;                        // index variable
    boolean maximizeFlag;         // true if we maximize our main window
    Insets textInsets;            // margins on text input fields
    int windowHeight, windowLeft, windowTop, windowWidth;
                                  // position and size for <mainFrame>
    String word;                  // one parameter from command line

    /* Initialize variables used by both console and GUI applications. */

    cancelFlag = false;           // don't cancel unless user complains
    commonFontName = SYSTEM_FONT; // default to normal font on local system
    commonFontSize = 18;          // preferred font size of buttons, labels
    mainFrame = null;             // during setup, there is no GUI window
    maximizeFlag = false;         // by default, don't maximize our main window
    mswinFlag = System.getProperty("os.name").startsWith("Windows");
    outputFontName = "Verdana";   // preferred font name for output text area
    outputFontSize = 18;          // starting font size for output text area
    windowHeight = DEFAULT_HEIGHT; // default window position and size
    windowLeft = DEFAULT_LEFT;
    windowTop = DEFAULT_TOP;
    windowWidth = DEFAULT_WIDTH;

    /* Initialize number formatting styles. */

    formatPointOne = NumberFormat.getInstance(); // current locale
    formatPointOne.setMaximumFractionDigits(1); // maybe one decimal digit
    formatPointOne.setMinimumFractionDigits(0); // and possibly no decimal

    formatPointThree = NumberFormat.getInstance();
    formatPointThree.setMaximumFractionDigits(3); // always 3 decimal digits
    formatPointThree.setMinimumFractionDigits(3);

    formatPointFive = NumberFormat.getInstance();
    formatPointFive.setMaximumFractionDigits(5); // always 5 decimal digits
    formatPointFive.setMinimumFractionDigits(5);

    /* Check command-line parameters for options. */

    for (i = 0; i < args.length; i ++)
    {
      word = args[i].toLowerCase(); // easier to process if consistent case
      if (word.length() == 0)
      {
        /* Ignore empty parameters, which are more common than you might think,
        when programs are being run from inside scripts (command files). */
      }

      else if (word.equals("?") || word.equals("-?") || word.equals("/?")
        || word.equals("-h") || (mswinFlag && word.equals("/h"))
        || word.equals("-help") || (mswinFlag && word.equals("/help")))
      {
        showHelp();               // show help summary
        System.exit(EXIT_UNKNOWN); // exit application after printing help
      }

      else if (word.startsWith("-u") || (mswinFlag && word.startsWith("/u")))
      {
        /* This option is followed by a font point size that will be used for
        GUI buttons, dialogs, labels, etc. */

        try                       // try to parse remainder as an integer
        {
          commonFontSize = Integer.parseInt(word.substring(2));
        }
        catch (NumberFormatException nfe) // if not a number or bad syntax
        {
          commonFontSize = -1;    // set result to an illegal value
        }
        if ((commonFontSize < 10) || (commonFontSize > 99))
        {
          System.err.println("Dialog font size must be from 10 to 99: "
            + args[i]);           // notify user of our arbitrary limits
          showHelp();             // show help summary
          System.exit(EXIT_FAILURE); // exit application after printing help
        }
        outputFontSize = commonFontSize; // same for output text area
      }

      else if (word.startsWith("-w") || (mswinFlag && word.startsWith("/w")))
      {
        /* This option is followed by a list of four numbers for the initial
        window position and size.  All values are accepted, but small heights
        or widths will later force the minimum packed size for the layout. */

        Pattern pattern = Pattern.compile(
          "\\s*\\(\\s*(\\d{1,5})\\s*,\\s*(\\d{1,5})\\s*,\\s*(\\d{1,5})\\s*,\\s*(\\d{1,5})\\s*\\)\\s*");
        Matcher matcher = pattern.matcher(word.substring(2)); // parse option
        if (matcher.matches())    // if option has proper syntax
        {
          windowLeft = Integer.parseInt(matcher.group(1));
          windowTop = Integer.parseInt(matcher.group(2));
          windowWidth = Integer.parseInt(matcher.group(3));
          windowHeight = Integer.parseInt(matcher.group(4));
        }
        else                      // bad syntax or too many digits
        {
          System.err.println("Invalid window position or size: " + args[i]);
          showHelp();             // show help summary
          System.exit(EXIT_FAILURE); // exit application after printing help
        }
      }

      else if (word.equals("-x") || (mswinFlag && word.equals("/x")))
        maximizeFlag = true;      // true if we maximize our main window

      else                        // parameter is not a recognized option
      {
        System.err.println("Option not recognized: " + args[i]);
        showHelp();               // show help summary
        System.exit(EXIT_FAILURE); // exit application after printing help
      }
    }

    /* Initialize shared graphical objects. */

    action = new MetricSaeSizes5User(); // create our shared action listener
    commonFont = new Font(commonFontName, Font.PLAIN, commonFontSize);
    emptyBorder = BorderFactory.createEmptyBorder(); // for removing borders
    fileChooser = new JFileChooser(); // create our shared file chooser
    outputFont = new Font(outputFontName, Font.PLAIN, outputFontSize);
    textInsets = new Insets(2, 4, 2, 4); // margins on text input fields

    /* If we ask for a font that is not installed on the local system, Java
    replaces this with its "Dialog" font.  This is only of concern for the
    output text area, because we have a combo box with font names and would
    like the correct name to be selected. */

    if (outputFont.getFamily().equals(outputFontName) == false)
    {
      outputFontName = SYSTEM_FONT; // replace with known good font
      outputFont = new Font(outputFontName, Font.PLAIN, outputFontSize);
    }

    /* Create the graphical interface as a series of smaller panels inside
    bigger panels.  The intermediate panel names are of no lasting importance
    and hence are only numbered (panel261, label354, etc). */

    /* Create a vertical box to stack buttons and options. */

    JPanel panel01 = new JPanel();
    panel01.setLayout(new BoxLayout(panel01, BoxLayout.Y_AXIS));

    /* Create a horizontal panel for the action buttons. */

    JPanel panel11 = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));

    startButton = new JButton("Start");
    startButton.addActionListener(action);
    startButton.setFont(commonFont);
    startButton.setMnemonic(KeyEvent.VK_S);
    panel11.add(startButton);

    panel11.add(Box.createHorizontalStrut(40));

    cancelButton = new JButton("Cancel");
    cancelButton.addActionListener(action);
    cancelButton.setEnabled(false);
    cancelButton.setFont(commonFont);
    cancelButton.setMnemonic(KeyEvent.VK_C);
    panel11.add(cancelButton);

    panel11.add(Box.createHorizontalStrut(40));

    exitButton = new JButton("Exit");
    exitButton.addActionListener(action);
    exitButton.setFont(commonFont);
    exitButton.setMnemonic(KeyEvent.VK_X);
    panel11.add(exitButton);

    panel01.add(panel11);
    panel01.add(Box.createVerticalStrut(12)); // space between panels

    /* Options for the metric sizes. */

    JPanel panel21 = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));

    JLabel label22 = new JLabel("Metric sizes from  ");
    label22.setFont(commonFont);
    panel21.add(label22);

    metDialogFirst = new JTextField("3", 5);
    metDialogFirst.setFont(commonFont);
    metDialogFirst.setHorizontalAlignment(JTextField.CENTER);
    metDialogFirst.setMargin(textInsets);
    panel21.add(metDialogFirst);

    JLabel label23 = new JLabel("  to  ");
    label23.setFont(commonFont);
    panel21.add(label23);

    metDialogLast = new JTextField("32", 5);
    metDialogLast.setFont(commonFont);
    metDialogLast.setHorizontalAlignment(JTextField.CENTER);
    metDialogLast.setMargin(textInsets);
    panel21.add(metDialogLast);

    JLabel label24 = new JLabel("  mm in units of  ");
    label24.setFont(commonFont);
    panel21.add(label24);

    metDialogUnits = new JComboBox(MET_UNIT_CHOICES);
    metDialogUnits.setEditable(false);
    metDialogUnits.setFont(commonFont);
    metDialogUnits.setSelectedItem(MET_UNIT_DEFAULT);
    panel21.add(metDialogUnits);

    JLabel label25 = new JLabel("  mm.");
    label25.setFont(commonFont);
    panel21.add(label25);

    panel01.add(panel21);
    panel01.add(Box.createVerticalStrut(12)); // space between panels

    /* Options for the SAE sizes. */

    JPanel panel31 = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));

    JLabel label32 = new JLabel("SAE sizes from  ");
    label32.setFont(commonFont);
    panel31.add(label32);

    saeDialogFirst = new JTextField("1/8", 5);
    saeDialogFirst.setFont(commonFont);
    saeDialogFirst.setHorizontalAlignment(JTextField.CENTER);
    saeDialogFirst.setMargin(textInsets);
    panel31.add(saeDialogFirst);

    JLabel label33 = new JLabel("  to  ");
    label33.setFont(commonFont);
    panel31.add(label33);

    saeDialogLast = new JTextField("1-1/4", 5);
    saeDialogLast.setFont(commonFont);
    saeDialogLast.setHorizontalAlignment(JTextField.CENTER);
    saeDialogLast.setMargin(textInsets);
    panel31.add(saeDialogLast);

    JLabel label34 = new JLabel("  inch in units of  ");
    label34.setFont(commonFont);
    panel31.add(label34);

    saeDialogUnits = new JComboBox(SAE_UNIT_CHOICES);
    saeDialogUnits.setEditable(false);
    saeDialogUnits.setFont(commonFont);
    saeDialogUnits.setSelectedItem(SAE_UNIT_DEFAULT);
    panel31.add(saeDialogUnits);

    JLabel label35 = new JLabel("  inch.");
    label35.setFont(commonFont);
    panel31.add(label35);

    panel01.add(panel31);
    panel01.add(Box.createVerticalStrut(12)); // space between panels

    /* Options for the bias when rounding to whole numbers. */

    JPanel panel41 = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));

    JLabel label42 = new JLabel("Add bias  ");
    label42.setFont(commonFont);
    panel41.add(label42);

    biasDialog = new JTextField("0.0", 5);
    biasDialog.setFont(commonFont);
    biasDialog.setHorizontalAlignment(JTextField.CENTER);
    biasDialog.setMargin(textInsets);
    panel41.add(biasDialog);

    JLabel label43 = new JLabel(
      "  up to 0.5 when rounding units to whole numbers.");
    label43.setFont(commonFont);
    panel41.add(label43);

    panel01.add(panel41);
    panel01.add(Box.createVerticalStrut(12)); // space between panels

    /* Options for the display font and a button for saving the output text. */

    JPanel panel51 = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));

    JLabel label52 = new JLabel("Font: ");
    label52.setFont(commonFont);
    panel51.add(label52);

    fontNameDialog = new JComboBox(GraphicsEnvironment
      .getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
    fontNameDialog.setEditable(false); // user must select one of our choices
    fontNameDialog.setFont(commonFont);
    fontNameDialog.setSelectedItem(outputFontName); // select default font name
    fontNameDialog.addActionListener(action); // do last so don't fire early
    panel51.add(fontNameDialog);

    panel51.add(Box.createHorizontalStrut(5));

    fontSizeDialog = new JComboBox(FONT_SIZES); // list of available sizes
    fontSizeDialog.setEditable(false); // user must select one of our choices
    fontSizeDialog.setFont(commonFont);
    fontSizeDialog.setSelectedItem(String.valueOf(outputFontSize));
    fontSizeDialog.addActionListener(action); // do last so don't fire early
    panel51.add(fontSizeDialog);

    panel51.add(Box.createHorizontalStrut(40));

    saveButton = new JButton("Save Output...");
    saveButton.addActionListener(action);
    saveButton.setFont(commonFont);
    saveButton.setMnemonic(KeyEvent.VK_O);
    panel51.add(saveButton);

    panel01.add(panel51);
    panel01.add(Box.createVerticalStrut(15)); // space between panels

    /* Bind all of the buttons and options above into a single panel so that
    the layout does not change when the window changes. */

    JPanel panel61 = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
    panel61.add(panel01);

    /* Create a scrolling text area for the generated output. */

    outputText = new JTextArea(15, 40);
    outputText.setEditable(false); // user can't change this text area
    outputText.setFont(outputFont);
    outputText.setLineWrap(false); // don't wrap text lines
    outputText.setMargin(new Insets(5, 6, 5, 6)); // top, left, bottom, right
    outputText.setText(
      "\nCalculate how close metric bolt/nut/socket/wrench sizes are to"
      + "\nfractional/inch/SAE/standard sizes."
      + "\n\nChoose your options; then click the \"Start\" button."
      + "\n\nCopyright (c) 2025 by Keith Fenske.  By using this program, you"
      + "\nagree to terms and conditions of the Apache License and/or GNU"
      + "\nGeneral Public License.\n\n");

    JScrollPane panel71 = new JScrollPane(outputText);
    panel71.setBorder(emptyBorder); // no border necessary here

    /* Combine buttons and options with output text.  The text area expands and
    contracts with the window size. */

    JPanel panel91 = new JPanel(new BorderLayout(0, 0));
    panel91.add(panel61, BorderLayout.NORTH); // buttons and options
    panel91.add(panel71, BorderLayout.CENTER); // text area

    /* Create the main window frame for this application.  We supply our own
    margins using the edges of the frame's border layout. */

    mainFrame = new JFrame(PROGRAM_TITLE);
    Container panel92 = mainFrame.getContentPane(); // where content meets frame
    panel92.setLayout(new BorderLayout(0, 0));
    panel92.add(Box.createVerticalStrut(15), BorderLayout.NORTH); // top margin
    panel92.add(Box.createHorizontalStrut(5), BorderLayout.WEST); // left
    panel92.add(panel91, BorderLayout.CENTER); // actual content in center
    panel92.add(Box.createHorizontalStrut(5), BorderLayout.EAST); // right
    panel92.add(Box.createVerticalStrut(5), BorderLayout.SOUTH); // bottom

    mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    mainFrame.setLocation(windowLeft, windowTop); // normal top-left corner
    if ((windowHeight < MIN_FRAME) || (windowWidth < MIN_FRAME))
      mainFrame.pack();           // do component layout with minimum size
    else                          // the user has given us a window size
      mainFrame.setSize(windowWidth, windowHeight); // size of normal window
    if (maximizeFlag) mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
    mainFrame.validate();         // recheck application window layout
    mainFrame.setVisible(true);   // and then show application window

    /* Let the graphical interface run the application now. */

    startButton.requestFocusInWindow(); // give keyboard focus to this button

  } // end of main() method

// ------------------------------------------------------------------------- //

/*
  doCancelButton() method

  Call this method if the user wants to stop processing early, perhaps because
  it is taking too long.  We must cleanly terminate any secondary threads.
  Leave whatever output has already been generated in the output text area.
*/
  static void doCancelButton()
  {
    cancelFlag = true;            // tell other threads that all work stops now
    putOutput("Cancelled by user."); // print message and scroll
  }


/*
  doSaveButton() method

  Ask the user for an output file name, create or replace that file, and copy
  the contents of our output text area to that file.  The output file will be
  in the default character set for the system, so if there are special Unicode
  characters in the displayed text (Arabic, Chinese, Eastern European, etc),
  then you are better off copying and pasting the output text directly into a
  Unicode-aware application like Microsoft Word.
*/
  static void doSaveButton()
  {
    FileWriter output;            // output file stream
    File userFile;                // file chosen by the user

    /* Ask the user for an output file name. */

    fileChooser.resetChoosableFileFilters(); // remove any existing filters
    fileChooser.setDialogTitle("Save Output as Text File...");
    fileChooser.setFileHidingEnabled(true); // don't show hidden files
    fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    fileChooser.setMultiSelectionEnabled(false); // allow only one file
    if (fileChooser.showSaveDialog(mainFrame) != JFileChooser.APPROVE_OPTION)
      return;                     // user cancelled file selection dialog box
    userFile = fileChooser.getSelectedFile();

    /* See if we can write to the user's chosen file. */

    if (userFile.isDirectory())   // can't write to directories or folders
    {
      JOptionPane.showMessageDialog(mainFrame, (userFile.getName()
        + " is a directory or folder.\nPlease select a normal file."));
      return;
    }
    else if (userFile.isHidden()) // won't write to hidden (protected) files
    {
      JOptionPane.showMessageDialog(mainFrame, (userFile.getName()
        + " is a hidden or protected file.\nPlease select a normal file."));
      return;
    }
    else if (userFile.isFile() == false) // if file doesn't exist
    {
      /* Maybe we can create a new file by this name.  Do nothing here. */
    }
    else if (userFile.canWrite() == false) // file exists, but is read-only
    {
      JOptionPane.showMessageDialog(mainFrame, (userFile.getName()
        + " is locked or write protected.\nCan't write to this file."));
      return;
    }
    else if (JOptionPane.showConfirmDialog(mainFrame, (userFile.getName()
      + " already exists.\nDo you want to replace this with a new file?"))
      != JOptionPane.YES_OPTION)
    {
      return;                     // user cancelled file replacement dialog
    }

    /* Write lines to output file. */

    try                           // catch file I/O errors
    {
      output = new FileWriter(userFile); // try to open output file
      outputText.write(output);   // couldn't be much easier for writing!
      output.close();             // try to close output file
    }
    catch (IOException ioe)
    {
      putOutput("Can't write to text file: " + ioe.getMessage());
    }
  } // end of doSaveButton() method


/*
  doStartButton() method

  Do work as requested by the user.  This program is actually very quick, but
  to allow the same code to be re-used, we do the work in a secondary thread,
  and have a "Cancel" button to interrupt that other thread.
*/
  static void doStartButton()
  {
    /* Get and check user's input for options. */

    metValueFirst = parseSize(metDialogFirst.getText());
    metValueLast = parseSize(metDialogLast.getText());
    if ((metValueFirst < 0.0) || (metValueFirst > metValueLast) ||
      (metValueLast > 250.0))
    {
      JOptionPane.showMessageDialog(mainFrame,
        "Metric size range must be from 0.0 to 250.0 mm.");
      return;
    }

    saeValueFirst = parseSize(saeDialogFirst.getText());
    saeValueLast = parseSize(saeDialogLast.getText());
    if ((saeValueFirst < 0.0) || (saeValueFirst > saeValueLast) ||
      (saeValueLast > 10.0))
    {
      JOptionPane.showMessageDialog(mainFrame,
        "SAE size range must be from 0.0 to 10.0 inch.");
      return;
    }

    try { biasValue = Double.parseDouble(biasDialog.getText()); }
    catch (NumberFormatException nfe) { biasValue = -999.0; }
    if ((biasValue < -1.0) || (biasValue > +1.0))
    {
      JOptionPane.showMessageDialog(mainFrame,
        "Bias value must be from -1.0 to +1.0"); // no sentence period here
      return;
    }

    /* Get number of units per millimeter and per inch. */

    switch (metDialogUnits.getSelectedIndex()) // get units per millimeter
    {
      case (0): unitsPerMm = 10.0; break;
      case (1): unitsPerMm = 5.0; break;
      case (2): unitsPerMm = 2.0; break;
      case (3): unitsPerMm = 1.0; break;
      case (4): unitsPerMm = 0.5; break;
      case (5): unitsPerMm = 0.2; break;
      case (6): unitsPerMm = 0.1; break;
      default:  unitsPerMm = 1.0; break;
    }

    switch (saeDialogUnits.getSelectedIndex()) // get units per inch
    {
      case (0): unitsPerInch = 256; break;
      case (1): unitsPerInch = 128; break;
      case (2): unitsPerInch = 64; break;
      case (3): unitsPerInch = 32; break;
      case (4): unitsPerInch = 16; break;
      case (5): unitsPerInch = 8; break;
      case (6): unitsPerInch = 4; break;
      case (7): unitsPerInch = 2; break;
      default:  unitsPerInch = 16; break;
    }

    /* We have our options.  Disable the "Start" button until we are done, and
    enable a "Cancel" button in case our secondary thread runs for a long time
    and the user panics. */

    cancelButton.setEnabled(true); // enable button to cancel this processing
    cancelFlag = false;           // but don't cancel unless user complains
    outputText.setText("");       // clear output text area
    startButton.setEnabled(false); // suspend "Start" button until we are done

    doStartThread = new Thread(new MetricSaeSizes5User(), "doStartRunner");
    doStartThread.setPriority(Thread.MIN_PRIORITY);
                                  // use low priority for heavy-duty workers
    doStartThread.start();        // run separate thread to do the real work

  } // end of doStartButton() method


/*
  doStartRunner() method

  This method is called inside a separate thread by the runnable interface of
  our "user" class to do the work as requested by the user in the context of
  the "main" class.  By doing all the heavy-duty work in a separate thread, we
  won't stall the main thread that runs the graphical interface, and we allow
  the user to cancel the processing if it takes too long.
*/
  static void doStartRunner()
  {
    /* Call another method to process the data, so that it can return to us
    upon success or failure, and we still clean up correctly. */

    processData();                // should have a more exciting name

    /* We are done.  Turn off the "Cancel" button and allow the user to click
    the "Start" button again. */

    cancelButton.setEnabled(false); // disable "Cancel" button
    startButton.setEnabled(true); // enable "Start" button
  }


/*
  fractionString() method

  Convert an integer numerator and denominator into a fraction as a string.
  The numerator must be non-negative and the denominator is a power of two.
*/
  static String fractionString(int top, int bottom)
  {
    int denom, num, whole;        // individual parts of the fraction
    String result;                // what we return to the caller

    whole = top / bottom;         // integer part
    num = top % bottom;           // starting numerator for fraction
    denom = bottom;               // reduces while inside <while> loop
    while ((num > 0) && ((num % 2) == 0)) { num /= 2; denom /= 2; }
    if (num == 0) result = String.valueOf(whole); // example: one inch
    else if (whole == 0) result = num + "/" + denom; // example: 15/16
    else result = whole + "-" + num + "/" + denom; // example: 1-1/4
    return(result);
  }


/*
  parseSize() method

  The caller gives us a string that represents a non-negative number, and we
  return a floating-point value for that number.  The number may be an unsigned
  integer ("123"), have a decimal point such as "123.45" (no sign or exponent),
  or a fraction like "1-5/8" or "7/32" (no sign).  A negative value is returned
  if the number can not be parsed in one of these forms.

  If this method is called often, then the compiled regular expression could be
  saved in a class variable.
*/
  static double parseSize(String input)
  {
    int denom, num, whole;        // individual parts of a fraction
    Matcher matcher;              // resulting matches from <pattern>
    Pattern pattern;              // compiled regular expression
    double result;                // what we return to the caller

    result = -1.0;                // default to an illegal size (value)
    pattern = Pattern.compile(    // can anyone really read this regex?
      "\\s*(?:((?:\\d+\\.?\\d*)|(?:\\d*\\.\\d+))|((?:(\\d+)(?:\\s+|(?:\\s*[\\&\\+\\-]\\s*)))?(\\d+)\\s*[\\/\\:]\\s*(\\d+)))\\s*");
    matcher = pattern.matcher(input); // try to parse input text
    if (matcher.matches())        // if the input has proper syntax
    {
      try                         // catch excessively large digit strings
      {
        if ((matcher.group(1) != null) && (matcher.group(1).length() > 0))
          result = Double.parseDouble(matcher.group(1)); // not a fraction
        else if ((matcher.group(2) != null) && (matcher.group(2).length() > 0))
        {                         // fraction with optional integer part
          if ((matcher.group(3) != null) && (matcher.group(3).length() > 0))
            whole = Integer.parseInt(matcher.group(3));
          else whole = 0;         // no integer part for this fraction
          num = Integer.parseInt(matcher.group(4)); // top
          denom = Integer.parseInt(matcher.group(5)); // bottom
          if (denom > 0)          // can't divide by zero
            result = (double) whole + ((double) num / (double) denom);
        }
      }
      catch (NumberFormatException nfe) { /* do nothing */ }
    }
    return(result);

  } // end of parseSize() method


/*
  processData() method

  Convert from metric to SAE and the reverse.  All options must already be
  parsed, checked for proper values, and saved in our class variables.  This
  code is a mix of integer and floating-point arithmetic, and it really helps
  to understand the difference here.

  There is nothing inherently GUI about this method, and it can be run from a
  console application so long as <putOutput> does something reasonable.
*/
  static void processData()
  {
    int firstUnit, lastUnit, thisUnit; // index variables
    String flag;                  // quick visual for how close
    double metExact;              // exact metric size in millimeters
    double metRound;              // metric size after rounding (mm)
    int metUnits;                 // number of metric units (rounded)
    double ratio;                 // rounded divided by exact
    double saeExact;              // exact SAE size in inches
    double saeRound;              // SAE size after rounding (inch)
    int saeUnits;                 // number of SAE units (rounded)

    /* Convert metric to SAE.  Sizes close to zero are treated as a minimum of
    one measurement unit.  So, yes, you can put zero as a starting size. */

    putOutput("");                // blank line
    putOutput("Metric/millimeter to fractional/inch/SAE/standard:");
    firstUnit = (int) Math.round(metValueFirst * unitsPerMm);
    firstUnit = Math.max(firstUnit, 1); // zero becomes zero and is boring
    lastUnit = (int) Math.round(metValueLast * unitsPerMm);
    lastUnit = Math.max(lastUnit, 1);
    for (thisUnit = firstUnit; thisUnit <= lastUnit; thisUnit ++)
    {
      if (cancelFlag) return;     // stop if user hit the panic button
      metExact = (double) thisUnit / unitsPerMm;
      saeExact = metExact / MM_PER_INCH; // exact size in inches
      saeUnits = (int) Math.round(saeExact * unitsPerInch + biasValue);
      saeUnits = Math.max(saeUnits, 1);
      saeRound = (double) saeUnits / (double) unitsPerInch;
      ratio = saeRound / saeExact; // ratio of rounded to exact
      if ((ratio > 0.997) && (ratio < 1.003)) flag = " very good";
      else if ((ratio > 0.992) && (ratio < 1.008)) flag = " good";
      else flag = "";             // nothing special, not close enough
      putOutput(formatPointOne.format(metExact) + " mm = "
        + formatPointThree.format(saeExact * unitsPerInch) + " / "
        + unitsPerInch + " inch, rounded to "
        + fractionString(saeUnits, unitsPerInch) + " has ratio "
        + formatPointFive.format(ratio) + flag);
    }

    /* Convert SAE to metric. */

    putOutput("");                // blank line
    putOutput("Fractional/inch/SAE/standard to metric/millimeter:");
    firstUnit = (int) Math.round(saeValueFirst * unitsPerInch);
    firstUnit = Math.max(firstUnit, 1); // zero becomes zero and is boring
    lastUnit = (int) Math.round(saeValueLast * unitsPerInch);
    lastUnit = Math.max(lastUnit, 1);
    for (thisUnit = firstUnit; thisUnit <= lastUnit; thisUnit ++)
    {
      if (cancelFlag) return;     // stop if user hit the panic button
      saeExact = (double) thisUnit / (double) unitsPerInch;
      metExact = saeExact * MM_PER_INCH; // exact size in millimeters
      metUnits = (int) Math.round(metExact * unitsPerMm + biasValue);
      metUnits = Math.max(metUnits, 1);
      metRound = (double) metUnits / unitsPerMm;
      ratio = metRound / metExact; // ratio of rounded to exact
      if ((ratio > 0.997) && (ratio < 1.003)) flag = " very good";
      else if ((ratio > 0.992) && (ratio < 1.008)) flag = " good";
      else flag = "";             // nothing special, not close enough
      putOutput(fractionString(thisUnit, unitsPerInch) + " inch = "
        + formatPointThree.format(metExact) + " mm, rounded to "
        + formatPointOne.format(metRound) + " has ratio "
        + formatPointFive.format(ratio) + flag);
    }
  } // end of processData() method


/*
  putOutput() method

  Append a complete line of text to the end of the output text area.  We add a
  newline character at the end of the line, not the caller.  By forcing all
  output to go through this same method, one complete line at a time, the
  generated output is cleaner and can be redirected.

  The output text area is forced to scroll to the end, after the text line is
  written, by selecting character positions that are much too large (and which
  are allowed by the definition of the JTextComponent.select() method).  This
  is easier and faster than manipulating the scroll bars directly.  However, it
  does cancel any selection that the user might have made, for example, to copy
  text from the output area.
*/
  static void putOutput(String text)
  {
    if (mainFrame == null)        // during setup, there is no GUI window
      System.out.println(text);   // console output goes onto standard output
    else
    {
      outputText.append(text + "\n"); // graphical output goes into text area
      outputText.select(999999999, 999999999); // force scroll to end of text
    }
  }


/*
  showHelp() method

  Show the help summary.  This is a UNIX standard and is expected for all
  console applications, even very simple ones.
*/
  static void showHelp()
  {
    System.err.println();
    System.err.println(PROGRAM_TITLE);
    System.err.println();
    System.err.println("This is a graphical application.  You may give options on the command line:");
    System.err.println();
    System.err.println("  -? = -help = show summary of command-line syntax");
    System.err.println("  -u# = font size for buttons, dialogs, etc; example: -u16");
    System.err.println("  -w(#,#,#,#) = normal window position: left, top, width, height;");
    System.err.println("      example: -w(50,50,700,500)");
    System.err.println("  -x = maximize application window; default is normal window");
    System.err.println();
    System.err.println(COPYRIGHT_NOTICE);
//  System.err.println();

  } // end of showHelp() method


/*
  userButton() method

  This method is called by our action listener actionPerformed() to process
  buttons, in the context of the main MetricSaeSizes5 class.
*/
  static void userButton(ActionEvent event)
  {
    Object source = event.getSource(); // where the event came from
    if (source == cancelButton)   // "Cancel" button
    {
      doCancelButton();           // stop secondary processing thread
    }
    else if (source == exitButton) // "Exit" button
    {
      System.exit(0);             // always exit with zero status from GUI
    }
    else if (source == fontNameDialog) // font name for output text area
    {
      /* The font name will be valid, because we obtained a list of names from
      getAvailableFontFamilyNames() and the dialog box is not editable. */

      outputFontName = (String) fontNameDialog.getSelectedItem();
      outputFont = new Font(outputFontName, Font.PLAIN, outputFontSize);
      outputText.setFont(outputFont);
    }
    else if (source == fontSizeDialog) // point size for output text area
    {
      /* The font size will be valid, since we provide a list of sizes and the
      dialog box is not editable. */

      outputFontSize = Integer.parseInt((String) fontSizeDialog
        .getSelectedItem());
      outputFont = new Font(outputFontName, Font.PLAIN, outputFontSize);
      outputText.setFont(outputFont);
    }
    else if (source == saveButton) // "Save Output" button
    {
      doSaveButton();             // write output text area to a file
    }
    else if (source == startButton) // "Start" button
    {
      doStartButton();            // start doing what it is that we do (joke)
    }
    else                          // fault in program logic, not by user
    {
      System.err.println("Error in userButton(): unknown ActionEvent: "
        + event);                 // should never happen, so write on console
    }
  } // end of userButton() method

} // end of MetricSaeSizes5 class

// ------------------------------------------------------------------------- //

/*
  MetricSaeSizes5User class

  This class listens to input from the user and passes back event parameters to
  a static method in the main class.
*/

class MetricSaeSizes5User implements ActionListener, Runnable
{
  /* empty constructor */

  public MetricSaeSizes5User() { }

  /* button listener, dialog boxes, etc */

  public void actionPerformed(ActionEvent event)
  {
    MetricSaeSizes5.userButton(event);
  }

  /* separate heavy-duty processing thread */

  public void run()
  {
    MetricSaeSizes5.doStartRunner();
  }

} // end of MetricSaeSizes5User class

/* Copyright (c) 2025 by Keith Fenske.  Apache License or GNU GPL. */
