package dataLoader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextFormatter.Change;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Window;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.IntegerStringConverter;

public class Controller implements Initializable {

  private static final int DEFAULT_MPH = 10;
  private final Pattern angleOfAttackPattern = Pattern.compile("[+]?([-]?\\d+)");
  private final Pattern numberPattern = Pattern.compile("[+]?([-]?[\\d.]+),\\s[+]?([-]?[\\d.]+),\\s[+]?([-]?[\\d.]+)");
  private final FileChooser fileChooser = new FileChooser();
  private final FileChooser fileSaver = new FileChooser();
  @FXML
  public Button fileLoader;
  @FXML
  private TextField intervalNumber;
  @FXML
  private TextField startMPH;
  @FXML
  private TextField airDensity;
  @FXML
  private TextField referenceArea;
  private File startLocation = fileChooser.getInitialDirectory();

  /**
   * Button input handle getting the file information and concatenating it together in a clear way
   */
  public final void createCVSFile(ActionEvent event) throws IOException {
//    Gets the initial directory of the file chooser makes it easier if you are running the program multiple times
    fileChooser.setInitialDirectory(startLocation);

//    Opens window
    Window window = ((Node) event.getSource()).getScene().getWindow();
    List<File> files = fileChooser.showOpenMultipleDialog(window);

//    If the user did select a file
    if (files != null) {
//      Set the starting directory to be the directory of the selected file
      startLocation = files.get(0).getParentFile();

//      Load the file data
      HashMap<Integer, List<LiftDragData>> angleOfAttackToLiftDraftData = loadFiles(files);
      saveToCSV(window, angleOfAttackToLiftDraftData);

    }
  }

  /**
   * Retrieves the file data. Gets the angle of attack, the closest mph to each interval in the data and also the
   * associated lift and draft with that mph
   *
   * @param files the files to extract the data from
   */
  private HashMap<Integer, List<LiftDragData>> loadFiles(Iterable<File> files) throws IOException {
    HashMap<Integer, List<LiftDragData>> angleOfAttackToLiftDraftData = new HashMap<>();

//    Lists through the files
    for (File file : files) {
//      Gets the angle of attack from file
      int angleOfAttack = findAngleOfAttack(file);

//      Gets the mph/lift/drag from file
      List<LiftDragData> liftDragData = findLiftDragData(file);

//      Adds that data to list
      angleOfAttackToLiftDraftData.put(angleOfAttack, liftDragData);
    }

    return angleOfAttackToLiftDraftData;
  }

  /**
   * Saves the Angle of Attack, MPH, Lift and Drag all into a *.csv file in the same order as listed.
   *
   * @param window the window to use to display the save dialog
   * @param angleOfAttackToLiftDraftData the file data for the angle of attack, mph, lift and drag to be saved
   */
  private void saveToCSV(Window window, HashMap<Integer, List<LiftDragData>> angleOfAttackToLiftDraftData)
      throws IOException {
//    Sorts the angle of attacks by lowest to greatest
    Integer[] angleOfAttacks = angleOfAttackToLiftDraftData.keySet().toArray(new Integer[0]);
    Arrays.sort(angleOfAttacks);

//    Opens the save dialog
    fileSaver.setInitialDirectory(startLocation);
    File file = fileSaver.showSaveDialog(window);

//    If the user picks a file
    if (file != null) {
      startLocation = file.getParentFile();

//      Open the file and write to it
      try (FileWriter fileWriter = new FileWriter(file)) {
//        Created string builder for efficiency
        StringBuilder stringBuilder = new StringBuilder(
            "Angle of Attack, MPH, Lift, Drag, Lift/Drag, Coefficient of Lift, Coefficient of Drag\n");

//        Iterates through all the angle of attacks from lowest to highest
        for (Integer angleOfAttack : angleOfAttacks) {
//          Goes through all the data for that angle of attack
          for (LiftDragData liftDragData : angleOfAttackToLiftDraftData.get(angleOfAttack)) {
//            The data in the .csv will be formatted as "angleOfAttack, mph,lift, drag" exactly
            stringBuilder
                .append(String
                    .format("%d, %f, %f, %f, %f, %f, %f%n", angleOfAttack, liftDragData.getMph(),
                        liftDragData.getLift(),
                        liftDragData.getDrag(), liftDragData.getLiftDividedByDrag(),
                        liftDragData.getCoefficientOfLift(),
                        liftDragData.getCoefficientOfDrag()));
          }
        }

//        Writes all the data to the file
        fileWriter.write(stringBuilder.toString());
      }
    }
  }

  /**
   * Finds the lift and drag data from the closest mph of the mph interval from the file.
   *
   * @param file the file to extract the data from
   */
  private List<LiftDragData> findLiftDragData(File file) throws IOException {
    List<LiftDragData> liftDragDataList = new LinkedList<>();

    double airDensity = Double.parseDouble(this.airDensity.getText());
    double referenceArea = Double.parseDouble(this.referenceArea.getText());

//    To read through the file
    try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {

      String line;
      String previous = null;
      boolean startDataCollection = false;

      Matcher matcher;

      double previousMPH = 0;
      double previousLift = 0;
      double previousDrag = 0;

//      First interval starts at 0
      double currentInterval = Double.parseDouble(startMPH.getText());

//      Goes through the available lines in the file
      while ((line = bufferedReader.readLine()) != null) {
//        Checks if the line is the start of the data
        if ("Wind, Lift, Drag=".equals(previous) && "[".equals(line)) {
          startDataCollection = true;
        }

//        If it is in the data section
        if (startDataCollection) {
          matcher = numberPattern.matcher(line);

//          If the line contains the mph, lift, drag
          if (matcher.find()) {
//            retrieves the captured data from regex
            double mph = Double.parseDouble(matcher.group(1));
            double lift = Double.parseDouble(matcher.group(2));
            double drag = Double.parseDouble(matcher.group(3));

//            If it is the first interval
            if (currentInterval == 0) {
              liftDragDataList.add(new LiftDragData(mph, lift, drag, airDensity, referenceArea));

//              System.out.printf("%f %f %f%n", mph, lift, drag);

//              Increment the interval by the number in the textfield
              currentInterval += Double.parseDouble(intervalNumber.getText());
            } else if ((currentInterval >= previousMPH) && (currentInterval <= mph)) {
//              If the previous mph and the current mph are between the current interval. Ex: 45 < 50 < 51 interval == 50

//              Gets if the closest mph is the previous or the current mph has the smallest distance between the interval
              if ((currentInterval - previousMPH) < (mph - currentInterval)) {
                liftDragDataList
                    .add(new LiftDragData(previousMPH, previousLift, previousDrag, airDensity, referenceArea));

//                System.out.printf("%f %f %f%n", previousMPH, previousLift, previousDrag);
              } else {
                liftDragDataList.add(new LiftDragData(mph, lift, drag, airDensity, referenceArea));
//                System.out.printf("%f %f %f%n", mph, lift, drag);
              }

//              Increase the interval
              currentInterval += Double.parseDouble(intervalNumber.getText());
            }

            previousMPH = mph;
            previousLift = lift;
            previousDrag = drag;
          }

        }

        previous = line;
      }

//      This is for the scenario of if the last number is >= half the half of the incrementation to add it.
//      This is for the cases like the last point is 59.9 and the interval is 60
      if (previousMPH >= (currentInterval - (Double.parseDouble(intervalNumber.getText()) / 2.0))) {
        liftDragDataList.add(new LiftDragData(previousMPH, previousLift, previousDrag, airDensity, referenceArea));
//        System.out.printf("%f %f %f%n", previousMPH, previousLift, previousDrag);
      }
    }

    return liftDragDataList;
  }

  /**
   * Finds the angle of attack of a .JET. It uses either the title of the file where it looks for a number or it looks
   * in the comment section and also for a number.
   *
   * @param file the file to get the angle of attack from
   */
  private int findAngleOfAttack(File file) throws IOException {
//    Gets the file name example FileName.JET
    String name = file.getName();

//    Looks for a numbers inside the title
    Matcher matcher = angleOfAttackPattern.matcher(name);

    String angleOfAttack = "0";

//    If it did find a number
    if (matcher.find()) {
//      Gets the first number
      angleOfAttack = matcher.group(1);
    } else {
//      Finds the comment section and finds a number within it
      try (BufferedReader br = new BufferedReader(new FileReader(file))) {
        String line;
        boolean comment = false;
        String previous = null;

//        Reads through the lines
        while ((line = br.readLine()) != null) {
          // If it is the start of the comment section
          if ("[".equals(line)) {
            comment = true;
          } else if (comment && "Wind, Lift, Drag=".equals(line) && "]".equals(previous)) {
            break;
          }

//          If it is in the comment section
          if (comment) {
//            Check for it there is a number
            matcher = angleOfAttackPattern.matcher(line);
//            If there is get the first number. Terminate the loop
            if (matcher.find()) {
              angleOfAttack = matcher.group(1);
              break;
            }
          }

          previous = line;
        }
      }
    }
//    Converts the string into the number
    return Integer.parseInt(angleOfAttack);
  }

  @Override
  public final void initialize(URL location, ResourceBundle resources) {
    //Sets the extensions for the file choosers and the file saver
    fileChooser.getExtensionFilters().add(new ExtensionFilter("JET Files", "*.JET"));
    fileSaver.getExtensionFilters().add(new ExtensionFilter("CSV Files", "*.csv"));

//    Checks that the text is actually a number
    UnaryOperator<Change> integerFilter = change -> {
      String newText = change.getControlNewText();
      if (newText.matches("-?([1-9][0-9]*)?")) {
        return change;
      }
      return null;
    };
//    Checks that the text is actually a number
    UnaryOperator<Change> doubleFilter = change -> {
      String newText = change.getControlNewText();
      if (newText.matches("-?([0-9]*(\\.[0-9]*)?)?")) {
        return change;
      }
      return null;
    };

//    Adds a number text formatter checker
    intervalNumber.setTextFormatter(
        new TextFormatter<>(new IntegerStringConverter(), DEFAULT_MPH, integerFilter));
//    Adds a number text formatter checker
    startMPH.setTextFormatter(
        new TextFormatter<>(new IntegerStringConverter(), DEFAULT_MPH, integerFilter));
//    Adds a number text formatter checker
    airDensity.setTextFormatter(
        new TextFormatter<>(new DoubleStringConverter(), 1.0, doubleFilter));
//    Adds a number text formatter checker
    referenceArea.setTextFormatter(
        new TextFormatter<>(new DoubleStringConverter(), 1.0, doubleFilter));


  }

  private static class LiftDragData {

    private final double mph;
    private final double lift;
    private final double drag;
    private final double coefficientOfLift;
    private final double coefficientOfDrag;

    LiftDragData(double mph, double lift, double drag, double airDensity,
        double referenceArea) {
      this.mph = mph;
      this.lift = lift;
      this.drag = drag;

      coefficientOfLift = calculateCoefficient(lift, getFeetPerSeconds(), airDensity, referenceArea);
      coefficientOfDrag = calculateCoefficient(drag, getFeetPerSeconds(), airDensity, referenceArea);
    }

    private double calculateCoefficient(
        double lift, double feetPerSeconds, double airDensity, double referenceArea) {
      return (2 * lift) / (airDensity * (feetPerSeconds * feetPerSeconds) * referenceArea);
    }

    final double getFeetPerSeconds() {
      return mph * (88.0 / 60.0);
    }

    final double getMph() {
      return mph;
    }

    final double getLift() {
      return lift;
    }

    final double getDrag() {
      return drag;
    }

    final double getLiftDividedByDrag() {
      return lift / drag;
    }

    final double getCoefficientOfLift() {
      return coefficientOfLift;
    }

    final double getCoefficientOfDrag() {
      return coefficientOfDrag;
    }

    @Override
    public final String toString() {
      return "LiftDragData{" +
          "lift=" + lift +
          ", drag=" + drag +
          '}';
    }
  }
}
