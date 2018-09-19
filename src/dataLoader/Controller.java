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
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextFormatter.Change;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Window;
import javafx.util.converter.IntegerStringConverter;

public class Controller implements Initializable {

  public Button fileLoader;
  public TextField intervalNumber;
  private Pattern angleOfAttackPattern = Pattern.compile("[+]?([-]?\\d+)");
  private Pattern numberPattern = Pattern.compile("[+]?([-]?[\\d.]+),\\s[+]?([-]?[\\d.]+),\\s[+]?([-]?[\\d.]+)");
  private FileChooser fileChooser = new FileChooser();
  private FileChooser fileSaver = new FileChooser();
  private File startLocation = fileChooser.getInitialDirectory();

  public void createCVSFile(ActionEvent event) throws IOException {
    fileChooser.setInitialDirectory(startLocation);

    Window window = ((Button) event.getSource()).getScene().getWindow();
    List<File> file = fileChooser.showOpenMultipleDialog(window);

    if (file != null) {
      startLocation = file.get(0).getParentFile();

      loadFiles(window, file);
    }
  }

  private void loadFiles(Window window, List<File> files) throws IOException {
    HashMap<Integer, List<LiftDragData>> angleOfAttackToLiftDraftData = new HashMap<>();

    for (File file : files) {
      int angleOfAttack = findAngleOfAttack(file);

      List<LiftDragData> liftDragData = findLiftDragData(file);

      angleOfAttackToLiftDraftData.put(angleOfAttack, liftDragData);
    }

    saveToCSV(window, angleOfAttackToLiftDraftData);
  }

  private void saveToCSV(Window window, HashMap<Integer, List<LiftDragData>> angleOfAttackToLiftDraftData)
      throws IOException {
    Integer[] angleOfAttacks = angleOfAttackToLiftDraftData.keySet().toArray(new Integer[0]);
    Arrays.sort(angleOfAttacks);

    File file = fileSaver.showSaveDialog(window);

    if (file != null) {
      try (FileWriter fileWriter = new FileWriter(file)) {
        StringBuilder stringBuilder = new StringBuilder("Angle of Attack, MPH, Lift, Drag\n");

        for (Integer angleOfAttack : angleOfAttacks) {
          for (LiftDragData liftDragData : angleOfAttackToLiftDraftData.get(angleOfAttack)) {
            stringBuilder
                .append(String.format("%d, %f, %f, %f%n", angleOfAttack, liftDragData.getMph(), liftDragData.getLift(),
                    liftDragData.getDrag()));
          }
        }

        fileWriter.write(stringBuilder.toString());
      }
    }
  }

  private List<LiftDragData> findLiftDragData(File file) throws IOException {
    List<LiftDragData> liftDragDataList = new LinkedList<>();

    try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {

      String line;
      String previous = null;
      boolean startDataCollection = false;

      Matcher matcher;

      double previousMPH = 0;
      double previousLift = 0;
      double previousDrag = 0;

      double currentInterval = 0;

      while ((line = bufferedReader.readLine()) != null) {
        if ("Wind, Lift, Drag=".equals(previous) && "[".equals(line)) {
          startDataCollection = true;
        }

        if (startDataCollection) {
          matcher = numberPattern.matcher(line);

          if (matcher.find()) {
            double mph = Double.parseDouble(matcher.group(1));
            double lift = Double.parseDouble(matcher.group(2));
            double drag = Double.parseDouble(matcher.group(3));

            if (currentInterval == 0) {
              liftDragDataList.add(new LiftDragData(mph, lift, drag));

//              System.out.printf("%f %f %f%n", mph, lift, drag);

              currentInterval += Double.parseDouble(intervalNumber.getText());
            } else if (currentInterval >= previousMPH && currentInterval <= mph) {
              if (currentInterval - previousMPH < mph - currentInterval) {
                liftDragDataList.add(new LiftDragData(previousMPH, previousLift, previousDrag));

//                System.out.printf("%f %f %f%n", previousMPH, previousLift, previousDrag);
              } else {
                liftDragDataList.add(new LiftDragData(mph, lift, drag));
//                System.out.printf("%f %f %f%n", mph, lift, drag);
              }

              currentInterval += Double.parseDouble(intervalNumber.getText());
            }

            previousMPH = mph;
            previousLift = lift;
            previousDrag = drag;
          }

        }

        previous = line;
      }

      if (previousMPH >= currentInterval - Double.parseDouble(intervalNumber.getText()) / 2.0) {
        liftDragDataList.add(new LiftDragData(previousMPH, previousLift, previousDrag));
//        System.out.printf("%f %f %f%n", previousMPH, previousLift, previousDrag);
      }
    }

    return liftDragDataList;
  }

  private int findAngleOfAttack(File file) throws IOException {
    String name = file.getName();
//    String name = "";

    Matcher matcher = angleOfAttackPattern.matcher(name);

    String angleOfAttack = "0";

    if (matcher.find()) {
      angleOfAttack = matcher.group(1);
    } else {
      try (BufferedReader br = new BufferedReader(new FileReader(file))) {
        String line;
        boolean comment = false;
        String previous = null;

        while ((line = br.readLine()) != null) {
          // process the line.
          if (line.equals("[")) {
            comment = true;
          } else if (comment && line.equals("Wind, Lift, Drag=") && "]".equals(previous)) {
            break;
          }

          if (comment) {
            matcher = angleOfAttackPattern.matcher(line);
            if (matcher.find()) {
              angleOfAttack = matcher.group(1);
              break;
            }
          }

          previous = line;
        }
      }
    }

    return Integer.parseInt(angleOfAttack);
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    fileChooser.getExtensionFilters().add(new ExtensionFilter("JET Files", "*.JET"));
    fileSaver.getExtensionFilters().add(new ExtensionFilter("CSV Files", "*.csv"));

    UnaryOperator<Change> integerFilter = change -> {
      String newText = change.getControlNewText();
      if (newText.matches("-?([1-9][0-9]*)?")) {
        return change;
      }
      return null;
    };

    intervalNumber.setTextFormatter(
        new TextFormatter<>(new IntegerStringConverter(), 10, integerFilter));


  }

  private class LiftDragData {

    private double mph;
    private double lift;
    private double drag;

    public LiftDragData(double mph, double lift, double drag) {
      this.mph = mph;
      this.lift = lift;
      this.drag = drag;
    }

    public double getMph() {
      return mph;
    }

    public double getLift() {
      return lift;
    }

    public double getDrag() {
      return drag;
    }

    @Override
    public String toString() {
      return "LiftDragData{" +
          "lift=" + lift +
          ", drag=" + drag +
          '}';
    }
  }
}
