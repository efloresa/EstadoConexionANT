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

/**
 *
 * @author erik.flores
 */
public class NotificacionEstadoConexionANT {
    
    

    public static Connection conectar() throws ClassNotFoundException, SQLException {
        String url = "jdbc:oracle:thin:@srvdbatm.atm.local:1521:srvbdatm"; // Cambia según tu configuración
        String usuario = "AXISATM";
        String contraseña = "NSVDLMSVCE";

        Class.forName("oracle.jdbc.driver.OracleDriver");
        return DriverManager.getConnection(url, usuario, contraseña);
    }

    public boolean isDatosAplicacionAvailable(String wsdlAddress) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(wsdlAddress);
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
    }    
    
    public static String getStatusConnectionANT(){
        
        Connection conexion = null;
        
        String strStatusConnectionANT = "N";
        String strNumnero = "";
        String strCaracter = "";
        String srtFechaDesde = "";
        String strFechaHasta = "";
        String srtError = "";

        try {
            conexion = conectar();
            System.out.println("Conexión exitosa a Oracle.");
            
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
            
            
            
            if (srtError == null){
                strStatusConnectionANT = strCaracter;
            }
            
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
    
    public void setStatusConnectionANT(){
        
        Connection conexion = null;
        String strStatusConnectionANT = "";
        String strExito = "";
        String strMensaje = "";
        
        try {
            conexion = conectar();
            System.out.println("Conexión exitosa a Oracle.");
            
            String sql = "{call GCP_CONTROL_ONLINE_ANT(?,?,?,?,?) }"; // Cambia el nombre de la función
            CallableStatement cst = conexion.prepareCall(sql);
            
            // Configura los parámetros de entrada y salida
            cst.setString(1, "CEX"); // 
            cst.setString(2, "ANT"); // 
            cst.registerOutParameter(3, Types.VARCHAR); // 
            cst.registerOutParameter(4, Types.VARCHAR); // 
            cst.setString(5, ""); // Nombre

            cst.execute();

            // Obtiene los resultados
            strExito = cst.getString(3);
            strMensaje = cst.getString(4);            
            
            
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

        while (iteracion < maxIteraciones) {
            try {
                // URL del servicio web (ajusta según tu caso)
                URL url = new URL("http://localhost:8080/MyWebService"); // Cambia la URL

                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000); // Timeout de 10 segundos

                // Realiza la conexión
                connection.connect();

                // Si el servicio web está disponible (código de respuesta 200)
                if (connection.getResponseCode() == 200) {
                    servicioDisponible = true;
                    break; // Rompe la iteración
                }

                // Pausa de 1.5 minutos
                TimeUnit.MINUTES.sleep(1);
            } catch (Exception e) {
                e.printStackTrace();
            }

            iteracion++;
        }

        if (servicioDisponible) {
            System.out.println("El servicio web está disponible.");
        } else {
            System.out.println("El servicio web no está disponible después de " + maxIteraciones + " iteraciones.");
        }

    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        String wsdlUrl = "http://sistematransitolocal.ant.gob.ec:6031/WebServices-DatosAplicacion-context-root/MetodosPort?WSDL"; // Replace with your WSDL URL
        
        LocalDate currentDate = LocalDate.now(); 
        
        System.out.println("Día de la semana (número): " + dayOfWeek(currentDate));
        System.out.println("Día de la semana (texto): " + getDayOfWeek(currentDate, Locale.getDefault()));
        System.out.println("Hora actual (número): " + getCurrentHour());
        System.out.println("Estado de conexion ANT actual: " + getStatusConnectionANT());
        
        NotificacionEstadoConexionANT tester = new NotificacionEstadoConexionANT();
        boolean isAvailable = tester.isDatosAplicacionAvailable(wsdlUrl);
        if (isAvailable) {
            System.out.println("Estado WEBServices ANT actual: OK" );
        } else {
            System.out.println("Estado WEBServices ANT actual: FAIL" );
        }        
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

