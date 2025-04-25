package viewmodel;

import dao.DbConnectivityClass;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.MajorEnum;
import model.Person;

import java.io.*;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

public class DB_GUI_Controller implements Initializable {

    @FXML private TextField first_name, last_name, department, email, imageURL;
    @FXML private ImageView img_view;
    @FXML private MenuBar menuBar;
    @FXML private TableView<Person> tv;
    @FXML private TableColumn<Person, Integer> tv_id;
    @FXML private TableColumn<Person, String> tv_fn, tv_ln, tv_department, tv_major, tv_email;
    @FXML private ComboBox<MajorEnum> majorComboBox;
    @FXML private Button addBtn, editItem, deleteItem;
    @FXML private Label statusLabel;

    private final DbConnectivityClass cnUtil = new DbConnectivityClass();
    private final ObservableList<Person> data = cnUtil.getData();

    private final BooleanProperty isFirstNameValid = new SimpleBooleanProperty(false);
    private final BooleanProperty isLastNameValid = new SimpleBooleanProperty(false);
    private final BooleanProperty isDepartmentValid = new SimpleBooleanProperty(false);
    private final BooleanProperty isEmailValid = new SimpleBooleanProperty(false);
    private final BooleanProperty isMajorSelected = new SimpleBooleanProperty(false);

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        tv_id.setCellValueFactory(new PropertyValueFactory<>("id"));
        tv_fn.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        tv_ln.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        tv_department.setCellValueFactory(new PropertyValueFactory<>("department"));
        tv_major.setCellValueFactory(new PropertyValueFactory<>("major"));
        tv_email.setCellValueFactory(new PropertyValueFactory<>("email"));
        tv.setItems(data);

        majorComboBox.setItems(FXCollections.observableArrayList(MajorEnum.values()));

        Platform.runLater(() -> {
            Scene scene = menuBar.getScene();
            if (scene != null) {
                scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                    if (event.isControlDown()) {
                        if (event.getCode() == KeyCode.E && !editItem.isDisabled()) {
                            editRecord();
                            event.consume();
                        } else if (event.getCode() == KeyCode.D && !deleteItem.isDisabled()) {
                            deleteRecord();
                            event.consume();
                        } else if (event.getCode() == KeyCode.R) {
                            clearForm();
                            event.consume();
                        } else if (event.getCode() == KeyCode.F) {
                            first_name.requestFocus();
                            event.consume();
                        }
                    }
                });
            }
        });

        validateInputs();

        addBtn.disableProperty().bind(
                isFirstNameValid.not()
                        .or(isLastNameValid.not())
                        .or(isDepartmentValid.not())
                        .or(isEmailValid.not())
                        .or(isMajorSelected.not())
        );

        editItem.setDisable(true);
        deleteItem.setDisable(true);
        tv.setOnMouseClicked(e -> {
            boolean rowSelected = tv.getSelectionModel().getSelectedItem() != null;
            editItem.setDisable(!rowSelected);
            deleteItem.setDisable(!rowSelected);
        });
    }

    private void validateInputs() {
        Pattern namePattern = Pattern.compile("^[A-Za-z]{2,}$");
        Pattern emailPattern = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

        first_name.textProperty().addListener((obs, oldVal, newVal) -> isFirstNameValid.set(namePattern.matcher(newVal).matches()));
        last_name.textProperty().addListener((obs, oldVal, newVal) -> isLastNameValid.set(namePattern.matcher(newVal).matches()));
        department.textProperty().addListener((obs, oldVal, newVal) -> isDepartmentValid.set(namePattern.matcher(newVal).matches()));
        email.textProperty().addListener((obs, oldVal, newVal) -> isEmailValid.set(emailPattern.matcher(newVal).matches()));
        majorComboBox.valueProperty().addListener((obs, oldVal, newVal) -> isMajorSelected.set(newVal != null));
    }

    @FXML protected void addNewRecord() {
        Person p = new Person(
                first_name.getText(),
                last_name.getText(),
                department.getText(),
                majorComboBox.getValue().name(),
                email.getText(),
                imageURL.getText()
        );
        cnUtil.insertUser(p);
        p.setId(cnUtil.retrieveId(p));
        data.add(p);
        showStatus("User added successfully.");
        clearForm();
    }

    @FXML protected void clearForm() {
        first_name.clear();
        last_name.clear();
        department.clear();
        majorComboBox.setValue(null);
        email.clear();
        imageURL.clear();
    }

    @FXML protected void selectedItemTV(MouseEvent mouseEvent) {
        Person p = tv.getSelectionModel().getSelectedItem();
        if (p != null) {
            first_name.setText(p.getFirstName());
            last_name.setText(p.getLastName());
            department.setText(p.getDepartment());
            email.setText(p.getEmail());
            imageURL.setText(p.getImageURL());
            try {
                majorComboBox.setValue(MajorEnum.valueOf(p.getMajor()));
            } catch (Exception ignored) {
                majorComboBox.setValue(null);
            }
        }
    }

    @FXML protected void editRecord() {
        Person p = tv.getSelectionModel().getSelectedItem();
        int index = data.indexOf(p);
        Person updated = new Person(
                p.getId(),
                first_name.getText(),
                last_name.getText(),
                department.getText(),
                majorComboBox.getValue().name(),
                email.getText(),
                imageURL.getText()
        );
        cnUtil.editUser(p.getId(), updated);
        data.set(index, updated);
        showStatus("User updated.");
    }

    @FXML protected void deleteRecord() {
        Person p = tv.getSelectionModel().getSelectedItem();
        if (p != null) {
            cnUtil.deleteRecord(p);
            data.remove(p);
            showStatus("User deleted.");
        }
    }

    @FXML protected void showImage() {
        File file = new FileChooser().showOpenDialog(img_view.getScene().getWindow());
        if (file != null) {
            img_view.setImage(new Image(file.toURI().toString()));
        }
    }

    @FXML protected void exportToCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
        File file = fileChooser.showSaveDialog(tv.getScene().getWindow());

        if (file != null) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                writer.println("First Name,Last Name,Department,Major,Email,ImageURL");
                for (Person p : data) {
                    writer.printf("%s,%s,%s,%s,%s,%s%n",
                            p.getFirstName(), p.getLastName(), p.getDepartment(),
                            p.getMajor(), p.getEmail(), p.getImageURL());
                }
                showStatus("Data exported to CSV.");
            } catch (IOException e) {
                e.printStackTrace();
                showStatus("Export failed.");
            }
        }
    }

    @FXML protected void importFromCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
        File file = fileChooser.showOpenDialog(tv.getScene().getWindow());

        if (file != null) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line = reader.readLine();
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",", -1);
                    if (parts.length == 6) {
                        Person p = new Person(
                                parts[0].trim(), parts[1].trim(), parts[2].trim(),
                                parts[3].trim(), parts[4].trim(), parts[5].trim()
                        );
                        cnUtil.insertUser(p);
                        p.setId(cnUtil.retrieveId(p));
                        data.add(p);
                    }
                }
                showStatus("Data imported from CSV.");
            } catch (IOException e) {
                e.printStackTrace();
                showStatus("Import failed.");
            }
        }
    }

    private void showStatus(String msg) {
        if (statusLabel != null) statusLabel.setText(msg);
    }

    @FXML public void lightTheme(ActionEvent actionEvent) {
        Scene scene = menuBar.getScene();
        scene.getStylesheets().clear();
        scene.getStylesheets().add(getClass().getResource("/css/lightTheme.css").toExternalForm());
    }

    @FXML public void darkTheme(ActionEvent actionEvent) {
        Scene scene = menuBar.getScene();
        scene.getStylesheets().clear();
        scene.getStylesheets().add(getClass().getResource("/css/darkTheme.css").toExternalForm());
    }

    @FXML public void displayAbout() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/about.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(root, 600, 500));
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML public void logOut(ActionEvent actionEvent) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/login.fxml"));
            Stage window = (Stage) menuBar.getScene().getWindow();
            window.setScene(new Scene(root, 900, 600));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML public void closeApplication() {
        Platform.exit();
    }
}
