package controller;

import app.App;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import model.Appointment;
import model.Customer;
import app.SchedulerRepository;
import model.User;
import utils.NumberTextField;
import utils.TimeChanger;

import java.net.URL;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.ResourceBundle;

public class Appointments implements Initializable {

    @FXML
    private ComboBox<User> userCombo;

    @FXML
    private ComboBox<Customer> customerCombo;

    @FXML
    private DatePicker datePicker;

    @FXML
    private NumberTextField startHourNumberTextField;
    @FXML
    private NumberTextField startMinNumberTextField;
    @FXML
    private ComboBox<String > startPeriodCombo;

    @FXML
    private NumberTextField endHourNumberTextField;
    @FXML
    private NumberTextField endMinNumberTextField;
    @FXML
    private ComboBox<String> endPeriodCombo;

    @FXML
    private TextField typeTextField;

    @FXML
    private Button appointmentSaveBtn;

    @FXML
    private Button appointmentCancelBtn;

    private Appointment selectedAppointment = null;
    private Main mainController;
    private String fieldVal;

    private ResourceBundle rb;
    private String consumerType;
    private String providerType;
    private String gatheringType;


    ////////////////////////////// Initialize
    @Override
    public void initialize(URL location, ResourceBundle resources) {

        rb = resources;
        setVerbiage();

        setFocusedPropertyListener(startHourNumberTextField);
        setFocusedPropertyListener(startMinNumberTextField);

        setFocusedPropertyListener(endHourNumberTextField);
        setFocusedPropertyListener(endMinNumberTextField);

        appointmentSaveBtn.setDefaultButton(true);
        appointmentCancelBtn.setCancelButton(true);
    }

    private void setVerbiage(){
        consumerType = rb.getString("consumerType");
        providerType = rb.getString("providerType");
        gatheringType = rb.getString("gatheringType");

    }

    private void setFocusedPropertyListener(NumberTextField ntf){
        ntf.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if(newVal) this.fieldVal = ntf.getText();// save field value when focused
            if(!newVal){
                DecimalFormat formatter = new DecimalFormat("00");
                String minStr = ntf.getText();
                int minNum = minStr.isEmpty() ? Integer.parseInt(this.fieldVal) : Integer.parseInt(minStr);
                if (ntf.getTimeFieldType().toLowerCase().equals("min")) {
                    ntf.setText(formatter.format(minNum));
                } else {
                    ntf.setText(String.valueOf(minNum));
                }
            }
        });
    }

    ////////////////////////////


    private LocalDateTime getTimeInput(LocalDate date, String hour, String min, String period) {
        DecimalFormat formatter = new DecimalFormat("00");
        int hourNum = Integer.parseInt(hour);
        String startStr = date + " " + formatter.format(hourNum) + ":" +
                min + ":00 " + period;

        return TimeChanger.ldtFromString(startStr, "yyyy-MM-dd hh:mm:ss a");
    }

    public void clickSaveAppointment(ActionEvent actionEvent) {

        // extract from fields
        User user = userCombo.getSelectionModel().getSelectedItem();
        int userId;
        try {
            userId = user.getId();
        } catch (NullPointerException e){
            App.dialog(Alert.AlertType.INFORMATION, "Select " + providerType,
                    "No " + providerType.toLowerCase() + " selected",
                    "You must select a(n) " + providerType.toLowerCase() + " to schedule a(n) " +
                            gatheringType.toLowerCase() + " for");
            return;
        }
        String userName = user.getUserName();

        Customer customer = customerCombo.getSelectionModel().getSelectedItem();
        int customerId;

        try {
            customerId = customer.getCustomerId();
        } catch (NullPointerException e){
            App.dialog(Alert.AlertType.INFORMATION, "Select " + consumerType,
                    "No " + consumerType.toLowerCase() + " selected",
                    "You must select a " + consumerType.toLowerCase() + " to set up a(n) " +
                            gatheringType.toLowerCase() + " with");
            return;
        }
        String customerName = customer.getCustomerName();

        String type = typeTextField.getText();

        if(type.isEmpty()){
            App.dialog(Alert.AlertType.INFORMATION, "Enter Type",
                    "No type of " + gatheringType.toLowerCase() + " entered",
                    "You must enter a type of " + gatheringType.toLowerCase());
            return;
        }

        // convert the user input for time into LocalDateTime
        LocalDateTime start = getTimeInput(datePicker.getValue(), startHourNumberTextField.getText(),
                startMinNumberTextField.getText(), startPeriodCombo.getValue());
        LocalDateTime end = getTimeInput(datePicker.getValue(), endHourNumberTextField.getText(),
                endMinNumberTextField.getText(), endPeriodCombo.getValue());

        LocalDateTime businessHourStart = getTimeInput(datePicker.getValue(), "08", "00", "AM");
        LocalDateTime businessHourEnd = getTimeInput(datePicker.getValue(), "5", "00", "PM");

        long diff = ChronoUnit.MINUTES.between(start, end);
        if (diff <= 0) {
            App.dialog(Alert.AlertType.INFORMATION, "Invalid times", "End time must be after start time",
                    "You must enter an end time after the start time");
            return;
        }

        // negative value means first time is AFTER second time
        long diffStart = ChronoUnit.MINUTES.between(businessHourStart, start);
        long diffEnd = ChronoUnit.MINUTES.between(end, businessHourEnd);

        // if diffStart negative, start time is before business hour start
        // if diffEnd is negative, end time is after business hour end
        if(diffStart < 0 || diffEnd < 0){
            // Business hours are between 08:00 and 17:00 (8AM to 5PM)
            App.dialog(Alert.AlertType.INFORMATION, "Not Between Business Hours",
                    "Start and end time must be between business hours 8AM to 5PM",
                    "You must enter start and end times between business hours");
            return;
        }

        try {
            SchedulerRepository.findOverlappingAppointment(user, selectedAppointment, start, end);
        } catch (RuntimeException e){
            System.out.println(e.getMessage());
            App.dialog(Alert.AlertType.INFORMATION, "Overlapping " + gatheringType + " Times",
                    "The " + gatheringType.toLowerCase() + " being scheduled overlaps another set " +
                            gatheringType.toLowerCase(),
                    "A(n) " + gatheringType.toLowerCase() + " is already scheduled during this time.");
            return;
        }

        int index;
        if(selectedAppointment == null){
            selectedAppointment = new Appointment(0, customerId, userId, type, userName, customerName,
                                                    start, end);
            index = SchedulerRepository.createAppointment(selectedAppointment);
        } else {

            int selectedAppointmentIndex = SchedulerRepository.getAppointments().indexOf(selectedAppointment);

            selectedAppointment.setUserId(userId);
            selectedAppointment.setUserName(userName);
            selectedAppointment.setStart(start);
            selectedAppointment.setEnd(end);
            selectedAppointment.setType(type);

            index = SchedulerRepository.updateAppointment(selectedAppointmentIndex, selectedAppointment);
        }

        if(index > 0) {
            App.closeThisWindow(actionEvent);
            mainController.confirmAppointmentTableViewTypeAndRefresh();
        }
    }

    public void clickCancelAppointment(ActionEvent actionEvent) {
        Optional<ButtonType> result = App.dialog(Alert.AlertType.CONFIRMATION,
                "Cancel " + gatheringType, "Confirm cancel",
                "Are you sure you want to cancel?\n\n");

        if (result.isPresent() && result.get() == ButtonType.OK)
            App.closeThisWindow(actionEvent);
    }

    ////////////////////////////// Controller methods

    void setAppointment(Appointment appointment, Main mainController) {
        this.selectedAppointment = appointment;
        this.mainController = mainController;
    }

    void initializeFieldData(){

        DecimalFormat formatter = new DecimalFormat("00");
        DateTimeFormatter hour = DateTimeFormatter.ofPattern("h");// get hour (non-military)
        DateTimeFormatter period = DateTimeFormatter.ofPattern("a");// get AM or PM

        userCombo.setItems(SchedulerRepository.getUsers());
        customerCombo.setItems(SchedulerRepository.getCustomers());

        if(selectedAppointment == null) { // then we are making a new appointment

            LocalDateTime localDateTime = LocalDateTime.now();

            // set default time to today's date and current time
            datePicker.setValue(localDateTime.toLocalDate());
            startHourNumberTextField.setText(localDateTime.format(hour));
            startMinNumberTextField.setText(formatter.format(localDateTime.getMinute()));
            startPeriodCombo.setValue(localDateTime.format(period));

            LocalDateTime thirtyMinLater = localDateTime.plusMinutes(30);// add 30 minutes to make default end time
            endHourNumberTextField.setText(thirtyMinLater.format(hour));
            endMinNumberTextField.setText(formatter.format(thirtyMinLater.getMinute()));
            endPeriodCombo.setValue(thirtyMinLater.format(period));

        } else { // we are editing an appointment that already exists

            // set the consultant (user) for this appointment
            userCombo.getItems().forEach(user -> {
                if(user.getUserName().equals(selectedAppointment.getUserName()))
                    userCombo.setValue(user);
            });

            customerCombo.getItems().forEach(customer -> {
                if(customer.getCustomerName().equals(selectedAppointment.getCustomerName())){
                    customerCombo.setValue(customer);
                }
            });

            LocalDateTime start = selectedAppointment.getStart();
            datePicker.setValue(start.toLocalDate());
            startHourNumberTextField.setText(start.format(hour));
            startMinNumberTextField.setText(formatter.format(start.getMinute()));
            startPeriodCombo.setValue(start.format(period));

            LocalDateTime end = selectedAppointment.getEnd();
            endHourNumberTextField.setText(end.format(hour));
            endMinNumberTextField.setText(formatter.format(end.getMinute()));
            endPeriodCombo.setValue(end.format(period));

            typeTextField.setText(selectedAppointment.getType());

        }

    }
}
