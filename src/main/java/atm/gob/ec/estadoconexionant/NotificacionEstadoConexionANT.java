/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package atm.gob.ec.estadoconexionant;

import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Types;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author erik.flores
 */
public class NotificacionEstadoConexionANT {
    
    private static final Logger LOGGER = Logger.getLogger(NotificacionEstadoConexionANT.class.getName());

    public static Connection conectar() throws ClassNotFoundException, SQLException {
        String url = "jdbc:oracle:thin:@srvdbatm.atm.local:1521:srvbdatm"; // Cambia según tu configuración
        String usuario = "AXISATM";
        String contraseña = "NSVDLMSVCE";

        Class.forName("oracle.jdbc.driver.OracleDriver");
        return DriverManager.getConnection(url, usuario, contraseña);
    }

    public boolean isDatosAplicacionAvailable() {
        String wsdlUrl = "http://sistematransitolocal.ant.gob.ec:6031/WebServices-DatosAplicacion-context-root/MetodosPort?WSDL"; // Replace with your WSDL URL
        
        HttpURLConnection connection = null;
        try {
            URL url = new URL(wsdlUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.getInputStream();
            return connection.getResponseCode() == 200;
        } catch (Exception e) {
//            e.printStackTrace();
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        
//                // URL del servicio web (ajusta según tu caso)
//                URL url = new URL("http://localhost:8080/MyWebService"); // Cambia la URL
//
//                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//                connection.setRequestMethod("GET");
//                connection.setConnectTimeout(10000); // Timeout de 10 segundos
//
//                // Realiza la conexión
//                connection.connect();
//
//                // Si el servicio web está disponible (código de respuesta 200)
//                if (connection.getResponseCode() == 200) {
//                    servicioDisponible = true;
//                    break; // Rompe la iteración
//                }
//
        
    }    
    
    public static String getStatusConnectionANT() throws SQLException, Exception{
        
        Connection conexion = null;
        
        String strStatusConnectionANT = "N";
        String strNumnero = "";
        String strCaracter = "";
        String srtFechaDesde = "";
        String strFechaHasta = "";
        String srtError = "";

        conexion = conectar();
        LOGGER.info("Conexión exitosa a Oracle.");

        String sql = "{call gep_parametros_doc(?,?,?,?,?,?,?) }"; // Cambia el nombre de la función
        CallableStatement cst = conexion.prepareCall(sql);

        // Configura los parámetros de entrada y salida
        cst.setString(1, "CEX"); // 
        cst.setString(2, "ANT"); // 
        cst.registerOutParameter(3, Types.VARCHAR); // 
        cst.registerOutParameter(4, Types.VARCHAR); // 
        cst.registerOutParameter(5, Types.VARCHAR); // 
        cst.registerOutParameter(6, Types.VARCHAR); // 
        cst.registerOutParameter(7, Types.VARCHAR); // 

        cst.execute();

        // Obtiene los resultados
        strNumnero = cst.getString(3);
        strCaracter = cst.getString(4);
        srtFechaDesde = cst.getString(5);
        strFechaHasta = cst.getString(6);
        srtError = cst.getString(7);          

        if (srtError == null)
            strStatusConnectionANT = strCaracter;
        
        if(conexion != null )                     
            conexion.close();
        
        return strStatusConnectionANT;
        
    }
    
    public static int dayOfWeek(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day.getValue();
    }

    public static String getDayOfWeek(LocalDate date, Locale locale) {
        DayOfWeek day = date.getDayOfWeek();
        return day.getDisplayName(TextStyle.FULL, locale);
    }

    
    public static int getCurrentHour(){
        // Obtener la hora actual usando java.time.LocalDate
        LocalTime horaActual = LocalTime.now();
        return horaActual.getHour();
    }
    
    public void setStatusConnectionANT(String paramOpcion, String paramParametro){
        
        Connection conexion = null;
        String strStatusConnectionANT = "";
        String strExito = "";
        String strMensaje = "";
        
        try {
            conexion = conectar();
            LOGGER.info("Conexión exitosa a Oracle.");
            
            String sql = "{call GCP_CONTROL_ONLINE_ANT(?,?,?,?,?) }"; // Cambia el nombre de la función
            CallableStatement cst = conexion.prepareCall(sql);
            
            // Configura los parámetros de entrada y salida
            cst.setString(1, paramOpcion); // 
            cst.setString(2, paramParametro); // 
            cst.registerOutParameter(3, Types.VARCHAR); // 
            cst.registerOutParameter(4, Types.VARCHAR); // 
            cst.setString(5, ""); // Nombre

            cst.execute();

            // Obtiene los resultados
            strExito = cst.getString(3);
            strMensaje = cst.getString(4);            
            
            LOGGER.info("Se ejectuto con exito? " + strExito + " " + strMensaje);
            
        } catch (Exception e) {
            e.printStackTrace();
        }finally{
            try {
                if(conexion != null ) 
                    conexion.close();
            } catch (SQLException ex) {
                System.exit(-1);
            }
        }
    }    
    
    public void ConsultaServicioWeb() {

        int maxIteraciones = 5;
        int iteracion = 0;
        boolean servicioDisponible = false;
        boolean blnEjecutarTarea = false;
        
        String strEstadoConexion = "";
        String strParametro = "ANT";
        String strOpcion = "";
        
        LocalDate currentDate = LocalDate.now(); 
        
        DayOfWeek dayOfWeek = LocalDate.now().getDayOfWeek();
        LocalTime currentTime = LocalTime.now();
        
        // Evaluar las condiciones
        if (dayOfWeek == DayOfWeek.SUNDAY) {
            // No hacer nada 
            blnEjecutarTarea = false;
            LOGGER.info("Hoy es domingo. No se realizará ninguna acción.");
        } else if (dayOfWeek == DayOfWeek.SATURDAY && currentTime.isAfter(LocalTime.of(7, 0)) && currentTime.isBefore(LocalTime.of(17, 0))) {
            // Acción para sábado entre 07:00 y 17:00
            blnEjecutarTarea = true;
            LOGGER.info("Hoy es sábado entre las 07:00 y las 17:00. Realizando acción...");
            // Realizar la acción correspondiente
        } else if (currentTime.isAfter(LocalTime.of(7, 0)) && currentTime.isBefore(LocalTime.of(19, 0))) {
            // Acción para lunes a viernes entre 07:00 y 19:00
            blnEjecutarTarea = true;
            LOGGER.info("Hoy es día laborable entre las 07:00 y las 19:00. Realizando acción...");
            // Realizar la acción correspondiente
        } else {
            // No hacer nada
            blnEjecutarTarea = false;
            LOGGER.info("No se realizará ninguna acción en este momento.");
        }
        
        if (blnEjecutarTarea == true){
            try {            
                strEstadoConexion = getStatusConnectionANT();
            
                while (iteracion < maxIteraciones) {
                    
                    // Si el servicio web está disponible (código de respuesta 200)
                    if (isDatosAplicacionAvailable()) {
                        servicioDisponible = true;
                        break; // Rompe la iteración
                    }
                    
                    // Pausa de 1.5 minutos
                    TimeUnit.MINUTES.sleep(1);
                    
                    iteracion++;
                }
            
                if (servicioDisponible) {
                    LOGGER.info("El servicio web está disponible.");
                    if (strEstadoConexion.equals("N")){
                        LOGGER.info("Actualizar estado de conexion ANT ONLINE"); 
                        strOpcion = "S";
                        setStatusConnectionANT(strOpcion, strParametro);
                    }
                } else {
                    LOGGER.info("El servicio web no está disponible después de " + maxIteraciones + " iteraciones.");
                    if (strEstadoConexion.equals("S")){
                        LOGGER.info("Actualizar estado de conexion ANT OFFLINE");
                        strOpcion = "N";
                        setStatusConnectionANT(strOpcion, strParametro);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } 
        }
        
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        
        LocalDate currentDate = LocalDate.now(); 
        NotificacionEstadoConexionANT tester = new NotificacionEstadoConexionANT();
        
        LOGGER.info("Día de la semana (número): " + dayOfWeek(currentDate));
        LOGGER.info("Día de la semana (texto): " + getDayOfWeek(currentDate, Locale.getDefault()));
        LOGGER.info("Hora actual (número): " + getCurrentHour());
//        try {
//            LOGGER.info("Estado de conexion ANT actual: " + getStatusConnectionANT());
//        } catch (Exception ex) {
//            LOGGER.severe(ex.toString());
//        }
        
//        boolean isAvailable = tester.isDatosAplicacionAvailable();
//        if (isAvailable) {
//            LOGGER.info("Estado WEBServices ANT actual: OK" );
//        } else {
//            LOGGER.info("Estado WEBServices ANT actual: FAIL" );
//        }
        
        tester.ConsultaServicioWeb();
        
    }
  
//    public static void main(String[] args) {
//        // TODO code application logic here
//        String wsdlUrl = "http://sistematransitolocal.ant.gob.ec:6131/WebServices-DatosAplicacion-context-root/MetodosPort?WSDL"; // Replace with your WSDL URL
//        
//        try {
//            URL url = new URL(wsdlUrl);
//            QName qname = new QName("http://gen/", "MetodosService");
//
//            Service service = Service.create(url, qname);
//            System.out.println("El servicio web WSDL está disponible.");
//                        
//        } catch (WebServiceException e) {
//            System.out.println("El servicio web WSDL NO está disponible.");
//            e.printStackTrace();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }  
//    }
  
}

