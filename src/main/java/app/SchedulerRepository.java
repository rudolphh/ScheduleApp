package app;

import dao.mysql.AppointmentMysqlDao;
import dao.mysql.CustomerMysqlDao;
import dao.mysql.UserMysqlDao;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import model.Appointment;
import model.Customer;
import model.User;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class SchedulerRepository {

    private static final ObservableList<Appointment> appointments = FXCollections.observableArrayList();
    private static final ObservableList<Customer> customers = FXCollections.observableArrayList();
    private static final ObservableList<User> users = FXCollections.observableArrayList();
    private static final ObservableList<Appointment> reportAppointments = FXCollections.observableArrayList();
    private static User loggedUser = null;

    ///////////////////////// methods
    public static void initialize(){
        findAllAppointments(null);
        customers.addAll(CustomerMysqlDao.findAllCustomers());
        users.addAll(UserMysqlDao.findAllUsers());
    }

    ////////////// create
    public static int createAppointment(Appointment appointment){
        int index = AppointmentMysqlDao.createAppointment(appointment);
        appointments.add(appointment);
        return index;
    }

    public static int createCustomer(Customer customer){

        int customerId = 0;

        try {
            Customer createdCustomer = CustomerMysqlDao.create(customer, loggedUser.getUserName());
            customerId = createdCustomer.getCustomerId();

            if (customerId > 0)
                customers.add(createdCustomer);
            else throw new SQLException("No new client was created - Customers.java");

        } catch (SQLException e){
            e.printStackTrace();
            System.out.println(e.getMessage());
        }

        return customerId;
    }

    public static void setLoggedUser(User user) { loggedUser = user; }

    public static ObservableList<Appointment> getAppointments(){ return appointments; }
    public static ObservableList<Customer> getCustomers(){ return customers; }
    public static ObservableList<User> getUsers(){ return users; }
    public static ObservableList<Appointment> getReportAppointments(){ return reportAppointments; }

    ////////////////// update

    public static int updateAppointment(int listIndex, Appointment selectedAppointment){
        int dbIndex = AppointmentMysqlDao.updateAppointment(selectedAppointment);
        appointments.set(listIndex, selectedAppointment);
        return dbIndex;
    }

    public static int updateCustomer(int index, Customer selectedCustomer){

        int dbIndex = 0;
        try {
            dbIndex = CustomerMysqlDao.updateCustomer(selectedCustomer, loggedUser.getUserName());
            customers.set(index, selectedCustomer);
        } catch (SQLException e){
            e.printStackTrace();
            System.out.println(e.getMessage());
        }

        return dbIndex;
    }

    ///////////////// delete

    public static void deleteAppointment(Appointment selectedAppointment) {
        AppointmentMysqlDao.deleteAppointment(selectedAppointment.getAppointmentId());
        appointments.remove(selectedAppointment);
    }

    public static int deleteCustomer(Customer selectedCustomer){
        int rowsAffected = CustomerMysqlDao.deleteCustomer(selectedCustomer.getCustomerId());
        if(rowsAffected > 0)
            customers.remove(selectedCustomer);
        return rowsAffected;
    }


    ////////////// appointments

    public static int appointmentWithinFifteenMin() {
        return AppointmentMysqlDao.findAppointmentWithinFifteenMin(loggedUser);
    }

    public static void findAllAppointments(User user) {

        ObservableList<Appointment> appointmentsFound = AppointmentMysqlDao.findAllAppointments(user);
        if(user == null){
            appointments.addAll(appointmentsFound);
        } else {
            reportAppointments.addAll(appointmentsFound);
        }
    }

    public static void findAllAppointments(int monthStart){
        appointments.addAll(AppointmentMysqlDao.findAllAppointments(monthStart));
    }

    public static void findAllAppointments(int monthStart, int dateStart){
        appointments.addAll(AppointmentMysqlDao.findAllAppointments(monthStart, dateStart));
    }

    public static int findAppointmentTypes(int monthStart) {
       return AppointmentMysqlDao.findAppointmentTypes(monthStart);
    }


    ///////////// customers

    public static int findNewCustomers(LocalDate currentDate) {
        return CustomerMysqlDao.findNewCustomers(currentDate);
    }

    public static void findAllAppointmentsByType(String searchVal) {
        reportAppointments.addAll(AppointmentMysqlDao.findAllAppointmentsByType(searchVal));
    }

    public static void findOverlappingAppointment(User user, Appointment selectedAppointment,
                                                  LocalDateTime start, LocalDateTime end) {
        AppointmentMysqlDao.findOverlappingAppointment(user, selectedAppointment, start, end);
    }
}
