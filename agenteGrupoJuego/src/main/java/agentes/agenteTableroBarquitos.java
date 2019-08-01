/*
 * Copyright (C) 2019 manuelgallegochinchilla
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package agentes;

import GUI.MainFrameBarquitos;
import jade.content.Concept;
import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.onto.BeanOntologyException;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.proto.AchieveREInitiator;
import jade.proto.ContractNetInitiator;
import jade.proto.ProposeInitiator;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import juegosTablero.Vocabulario;
import static juegosTablero.Vocabulario.Color.AMARILLO;
import static juegosTablero.Vocabulario.Color.AZUL;
import static juegosTablero.Vocabulario.Color.ROJO;
import static juegosTablero.Vocabulario.Color.VERDE;
import juegosTablero.Vocabulario.Estado;
import juegosTablero.Vocabulario.ModoJuego;
import juegosTablero.Vocabulario.Puntuacion;
import static juegosTablero.Vocabulario.Puntuacion.DERROTA;
import static juegosTablero.Vocabulario.Puntuacion.VICTORIA;
import juegosTablero.aplicacion.OntologiaJuegoBarcos;
import juegosTablero.aplicacion.barcos.ColocarBarcos;
import juegosTablero.aplicacion.barcos.EstadoJuego;
import juegosTablero.aplicacion.barcos.Localizacion;
import juegosTablero.aplicacion.barcos.MovimientoEntregado;
import juegosTablero.aplicacion.barcos.PosicionBarcos;
import juegosTablero.aplicacion.barcos.ResultadoMovimiento;
import juegosTablero.dominio.elementos.ClasificacionJuego;
import juegosTablero.dominio.elementos.Juego;
import juegosTablero.dominio.elementos.Jugador;
import juegosTablero.dominio.elementos.PedirMovimiento;
import juegosTablero.dominio.elementos.Posicion;
import sun.java2d.opengl.OGLRenderQueue;
import util.ArrayConversor;

/**
 *
 * @author manuelgallegochinchilla
 */
public class agenteTableroBarquitos extends AgenteOntologiaJuegos{

    private MainFrameBarquitos ventanaTablero1;
    private MainFrameBarquitos ventanaTablero2;
    private String jugadorActivo;
    private List<Jugador> jugadores;
     private int minVictorias;
    private Vocabulario.ModoJuego modoJuego;
    private String idPartida;
    private int tamanoTableroX;
    private int tamanoTableroY;
    int [][] Tablero1; //0-No visitado, 1-Agua, 2-Tocado, 3-Barco inicial
    int [][] Tablero2;
    private AID agenteGrupo;
    private ArrayConversor<Jugador> conversorJugadores;
    private boolean primeraPasada, primeraPintada;
    private ArrayList<Integer> victorias;
    private int colaJugador1, colaJugador2;
    private boolean resetear;
    private boolean enviarclasificacion;
    FileWriter fichero;
    PrintWriter pw;
    boolean partidaJugada;
    private int partidaActual;
    private boolean pintarTurno;
    
    @Override
    protected void setup(){
        //Inicialización de variables
        super.setup();
        colaJugador1 = 0;
        partidaActual = 1;
        resetear = false;
        enviarclasificacion = false;
        colaJugador2 = 0;
        primeraPintada = true;
        pintarTurno = true;
        primeraPasada = true;
        victorias = new ArrayList<>();
        Object[] objetos = this.getArguments();
        ArrayConversor<Jugador> conversor2=new ArrayConversor<>();
        jugadores =   conversor2.fromJade2Java((jade.util.leap.ArrayList) objetos[0], Jugador.class);
        minVictorias = (int) objetos[1];
        modoJuego = (ModoJuego) objetos[2]; 
        idPartida = (String) objetos[3];
        agenteGrupo = (AID) objetos[4];
        partidaJugada = (boolean) objetos[5];
        jugadorActivo = "Jug1";
        conversorJugadores = new ArrayConversor();
        tamanoTableroX = Vocabulario.FILAS_BARCOS;
        tamanoTableroY = Vocabulario.COLUMNAS_BARCOS;
        Tablero1 = new int[tamanoTableroX][tamanoTableroY];
        Tablero2 = new int[tamanoTableroX][tamanoTableroY];
        ventanaTablero1 = new MainFrameBarquitos("Jugador1");
        ventanaTablero2 = new MainFrameBarquitos("Jugador2");
 
        
        for(int i = 0; i < tamanoTableroX; i ++){
            for(int j = 0; j < tamanoTableroY; j ++){
                Tablero1[i][j] = 0;
                ventanaTablero1.getTablero().colocarFicha(i, 
                                j, VERDE);
                ventanaTablero1.getTablero().colorearFicha();//VERDE para identificar que no hay barco inicialmente ni ha sido visitado
            }
        }
        ventanaTablero1.setVisible(true);
        
        for(int i = 0; i < tamanoTableroX; i ++){
            for(int j = 0; j < tamanoTableroY; j ++){
                Tablero2[i][j] = 0;
                ventanaTablero2.getTablero().colocarFicha(i, 
                                j, VERDE);
                ventanaTablero2.getTablero().colorearFicha();//VERDE para identificar que no hay barco inicialmente ni ha sido visitado
            }
        }
        ventanaTablero2.setVisible(true);
      
        addBehaviour(new TareaPrepararPartida());      
    }
    
    @Override 
    protected void takeDown(){
        //Desregristo del agente de las Páginas Amarillas
        super.takeDown();
        try {
            super.finalize();
        } catch (Throwable ex) {
            Logger.getLogger(agenteTableroBarquitos.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        //Se liberan los recursos y se despide
        System.out.println("Finaliza la ejecución de " + this.getName());
    }
    
    
    @Override
    public Ontology [] defineOntology() throws Exception{
        Ontology[] ret = {
            OntologiaJuegoBarcos.getInstance()
        };
        return ret;
    }
    
    @Override
    public String nombreRegistro(){
        return Vocabulario.NombreServicio.JUEGO_BARCOS.toString();
    }
    
    /**
     * Método para pintar cada cierto tiempo las fichas en el tablero
     */
    public class pintar extends TickerBehaviour{

        public pintar(Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
            if(pintarTurno){
                ventanaTablero1.getTablero().colorearFicha();
                pintarTurno = false;
            }else{
                ventanaTablero2.getTablero().colorearFicha(); 
                pintarTurno = true;
            }
                   
        }       
    }
    
    
    //PREPARACION PARTIDA (SOLO EN LOS BARQUITOS)
    /**Para los juegos de los Barcos y Dominó hay que realizar una preparación 
    previa de la partida antes de poder jugarla. El AgenteTablero se pondrá 
    en comunicación con los AgenteJugador para establecer la preparación 
    de la partida.
    Para la comunicación entre los agentes se utilizará el protocolo Propose.*/
    public class preparacionPartidaIniciador extends ProposeInitiator {

	public preparacionPartidaIniciador(Agent a, ACLMessage initiation) {
		super(a, initiation);
	}

        @Override
        protected void handleOutOfSequence(ACLMessage msg) {
            // Ha llegado un mensaje fuera de la secuencia del protocolo           
        }

        /**
         * Revisamos las propuestas recibidas, tanto afirmativas como negativas.
         *
         * @param responses
         */
        @Override
        protected void handleAllResponses(Vector responses) {
            if(responses.size() == 2){
                ContentElement mensaje1 = null;
                ContentElement mensaje2 = null;
                for(Iterator it=responses.iterator(); it.hasNext();){
                    try {
                        mensaje1 = agenteTableroBarquitos.this.extraerMensaje((ACLMessage) it.next());
                        mensaje2 = agenteTableroBarquitos.this.extraerMensaje((ACLMessage) it.next());
                    } catch (Exception ex) {
                        Logger.getLogger(agenteTableroBarquitos.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                    
                PosicionBarcos mov1 = null;
                PosicionBarcos mov2 = null;
                if(mensaje1 instanceof PosicionBarcos){

                    mov1 = (PosicionBarcos) mensaje1;
                    for(int i = 0; i < mov1.getLocalizacionBarcos().size(); i++){
                        Localizacion l1 = (Localizacion) mov1.getLocalizacionBarcos().get(i);
                        if(l1.getOrientacion().equals(Vocabulario.Orientacion.HORIZONTAL)){
                            int x = l1.getPosicion().getCoorX();
                            int y = l1.getPosicion().getCoorY();
                            for(int j = 0; j < l1.getBarco().getCasillas(); j++){
                                Tablero1[x+j][y] = 3;
                                ventanaTablero1.getTablero().colocarFicha(x+j, 
                                y, AMARILLO);
                                ventanaTablero1.getTablero().colorearFicha();//AMARILLO para identificar que hay barco
                                pw.println(y+","+(x+j)+","+AMARILLO+",jug2");
                            }
                            
                        }else{
                            int x = l1.getPosicion().getCoorX();
                            int y = l1.getPosicion().getCoorY();
                            for(int j = 0; j < l1.getBarco().getCasillas(); j++){
                                Tablero1[x][y+j] = 3;
                                ventanaTablero1.getTablero().colocarFicha(x, 
                                y+j, AMARILLO);
                                ventanaTablero1.getTablero().colorearFicha();//AMARILLO para identificar que hay barco
                                 pw.println((y+j)+","+x+","+AMARILLO+",jug2");
                            }                          
                        }
                    }                       
                }
                
                if(mensaje2 instanceof PosicionBarcos){
                    mov2 = (PosicionBarcos) mensaje2;
                    for(int i = 0; i < mov2.getLocalizacionBarcos().size(); i++){
                        Localizacion l1 = (Localizacion) mov2.getLocalizacionBarcos().get(i);
                        if(l1.getOrientacion().equals(Vocabulario.Orientacion.HORIZONTAL)){
                            int x = l1.getPosicion().getCoorX();
                            int y = l1.getPosicion().getCoorY();
                            for(int j = 0; j < l1.getBarco().getCasillas(); j++){
                                Tablero2[x+j][y] = 3;
                                ventanaTablero2.getTablero().colocarFicha(x+j, 
                                y, AMARILLO);
                                ventanaTablero2.getTablero().colorearFicha();//AMARILLO para identificar que hay barco
                                 pw.println(y+","+(x+j)+","+AMARILLO+",jug1");
                            }
                            
                        }else{
                            int x = l1.getPosicion().getCoorX();
                            int y = l1.getPosicion().getCoorY();
                            for(int j = 0; j < l1.getBarco().getCasillas(); j++){
                                Tablero2[x][y+j] = 3;
                                ventanaTablero2.getTablero().colocarFicha(x, 
                                y+j, AMARILLO);
                                ventanaTablero2.getTablero().colorearFicha();//AMARILLO para identificar que hay barco
                                 pw.println((y+j)+","+x+","+AMARILLO+",jug1");
                            }                          
                        }
                    }  
                    
                }
                try {
                    if(primeraPintada){
                        addBehaviour(new pintar(myAgent, 1000));
                        primeraPintada = false;
                    }
                    pedirMovimiento();
                } catch (Exception ex) {
                    Logger.getLogger(agenteTableroBarquitos.class.getName()).log(Level.SEVERE, null, ex);
                }
            }else{
                System.out.println("Algún jugador se ha caído");
            }
            
        }
    }
    

    //JUGAR PARTIDA
    /**Para jugar una partida se comunicará el AgenteTablero con los 
     * AgenteJugador que están implicados. Para ello utilizaremos el protocolo 
     * ContractNet para implementar la comunicación.*/
    public ACLMessage pedirMovimiento() throws Exception{
        Jugador jug = null;
         ACLMessage msg = new ACLMessage(ACLMessage.CFP);
        if(jugadorActivo.equals("Jug1")){
            jugadorActivo = "Jug2";
            jug = jugadores.get(1);
        }else{
            jugadorActivo = "Jug1";
            jug = jugadores.get(0);
        }  

        for(int i = 0; i < jugadores.size(); i++){
            msg.addReceiver(jugadores.get(i).getAgenteJugador());
        }
        
	msg.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
        PedirMovimiento object = new PedirMovimiento(new Juego(idPartida, minVictorias, modoJuego, Vocabulario.TipoJuego.BARCOS), jug);
        Action a = new Action(this.getAID(), object);

        agenteTableroBarquitos.this.mensajeFillContent(msg, a, OntologiaJuegoBarcos.getInstance());
        
        if(primeraPasada){
            addBehaviour(new jugarPartidaIniciador(this, msg));
            primeraPasada = false;
        }else{
            return msg;
        }
        return null;
    }

    /**
     * Clase iniciador del protocolo Contract-Net para comunicarse con los jugadores
     */
    private class jugarPartidaIniciador extends ContractNetInitiator {

        /**
         * Constructor parametrizado de la clase
         *
         * @param agente Agente que llama al protocolo
         * @param plantilla Mensaje CFP
         */
        public jugarPartidaIniciador(Agent agente, ACLMessage plantilla) {
            super(agente, plantilla);           
        }

        /**
         * Manejador de rechazos de proposiciones.
         *
         * @param rechazo El agente abandona
         */
        @Override
        protected void handleRefuse(ACLMessage rechazo) {
        //Hay abandono, por lo que enviamos la Clasificación al AgenteGrupoJuegos 
            
            //Como organizo la lista de puntuaciones
            //enviarClasificacion(agente, CFP_KEY);
        }
        
        @Override
        protected void handleAllResponses(Vector respuestas, Vector aceptados) { 
            if(respuestas.size() == 2){
                ContentElement contenidoTurno = null;   
                for(Iterator it=respuestas.iterator(); it.hasNext();){
                    ACLMessage mensajeTurno2 = (ACLMessage) it.next();
                    try {
                        
                        ContentElement contenidoTurno2 = agenteTableroBarquitos.this.extraerMensaje((ACLMessage) mensajeTurno2);
                        if(contenidoTurno2 != null){
                            contenidoTurno = contenidoTurno2;
                        }
                            
                    } catch (Exception ex) {
                       // Logger.getLogger(agenteTableroBarquitos.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                                  
                MovimientoEntregado mov = null;
                if(contenidoTurno instanceof MovimientoEntregado){
                    mov = (MovimientoEntregado) contenidoTurno;
                             
                    int fila = mov.getMovimiento().getCoorX();
                    int columna = mov.getMovimiento().getCoorY();
                    Posicion p = new Posicion(fila, columna);
                    ResultadoMovimiento resultado = new ResultadoMovimiento(mov.getJuego(), p, Vocabulario.Efecto.AGUA);
                    if(jugadorActivo.equals("Jug1")){
                        if(Tablero2[fila][columna] == 3){
                            if (comprobarMovimiento(fila,columna, Tablero2)){ //TRUE si es hundido
                                resultado = new ResultadoMovimiento(mov.getJuego(), p, Vocabulario.Efecto.HUNDIDO);
                            }else{
                                resultado = new ResultadoMovimiento(mov.getJuego(), p, Vocabulario.Efecto.TOCADO);
                            }
                            ventanaTablero2.getTablero().colocarFicha(fila, 
                                columna, ROJO); //ROJO para representar que ha tocado
                            pw.println(columna+","+fila+","+ROJO+",jug1");
                            
                        }else{
                            ventanaTablero2.getTablero().colocarFicha(fila, 
                                columna, AZUL); //AZUL para representar que ha sido agua
                            pw.println(columna+","+fila+","+AZUL+",jug1");
                            
                        }

                    }else{
                        if(Tablero1[fila][columna] == 3){
                            if (comprobarMovimiento(fila,columna, Tablero2)){ //TRUE si es hundido
                                resultado = new ResultadoMovimiento(mov.getJuego(), p, Vocabulario.Efecto.HUNDIDO);
                            }else{
                                resultado = new ResultadoMovimiento(mov.getJuego(), p, Vocabulario.Efecto.TOCADO);
                            }
                            
                            ventanaTablero1.getTablero().colocarFicha(fila, 
                                columna, ROJO); //ROJO para representar que ha tocado
                            pw.println(columna+","+fila+","+ROJO+",jug2");
                            
                        }else{
                            ventanaTablero1.getTablero().colocarFicha(fila, 
                                columna, AZUL); //AZUL para representar que ha sido agua
                            pw.println(columna+","+fila+","+AZUL+",jug2");
                            
                        }
                    }
                    
                    for(Iterator it=respuestas.iterator(); it.hasNext();){
                        ACLMessage replica = ((ACLMessage) it.next()).createReply();
                        replica.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                        try {
                            replica = agenteTableroBarquitos.this.mensajeFillContent(replica, resultado, OntologiaJuegoBarcos.getInstance());
                            
                        } catch (BeanOntologyException ex) {
                            Logger.getLogger(agenteTableroBarquitos.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (AgenteOntologiaJuegos.OntologiaDesconocida ex) {
                            Logger.getLogger(agenteTableroBarquitos.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (Codec.CodecException ex) {
                            Logger.getLogger(agenteTableroBarquitos.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (OntologyException ex) {
                            Logger.getLogger(agenteTableroBarquitos.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        aceptados.addElement(replica);                  
                    }
            }else{
                System.out.println("Algún jugador se ha caído");
            }
        }
    }
        
        /**
         * Manejador de los mensajes inform
         *
         * @param inform Mensaje recibido
         */
        @Override
        protected void handleInform(ACLMessage inform){

            ContentElement mensaje = null;
            try {
                mensaje = agenteTableroBarquitos.this.extraerMensaje(inform);
            } catch (Exception ex) {
                Logger.getLogger(agenteTableroBarquitos.class.getName()).log(Level.SEVERE, null, ex);
            }

            if(mensaje instanceof EstadoJuego){
                EstadoJuego estado = (EstadoJuego) mensaje;
                //Compruebo si ha ganado o se sigue jugando
                if(estado.getEstadoJuego().equals(Estado.GANADOR)){

                    if(inform.getSender().equals(jugadores.get(0).getAgenteJugador())){                     
                        victorias.add(1);                        
                    }else{                        
                        victorias.add(2);                       
                    }
                    try {
                        comprobarVictoria();
                    } catch (AgenteOntologiaJuegos.OntologiaDesconocida ex) {
                        Logger.getLogger(agenteTableroBarquitos.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (Codec.CodecException ex) {
                        Logger.getLogger(agenteTableroBarquitos.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (OntologyException ex) {
                        Logger.getLogger(agenteTableroBarquitos.class.getName()).log(Level.SEVERE, null, ex);
                    }

                }else if(estado.getEstadoJuego().equals(Estado.SEGUIR_JUGANDO)){
                    try {
                        reset(pedirMovimiento());
                    } catch (Exception ex) {
                        Logger.getLogger(agenteTableroBarquitos.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }else{
                System.err.println("agentes.agenteTableroConecta4.jugarPartidaIniciador.handleInform(): ERROR EN EL ESTADO DE JUEGO");
            }
        }
        
        

        @Override
        protected void handleFailure(ACLMessage failure){
             ContentElement mensaje = null;
            try {
                mensaje = agenteTableroBarquitos.this.extraerMensaje(failure);
            } catch (Exception ex) {
                Logger.getLogger(agenteTableroBarquitos.class.getName()).log(Level.SEVERE, null, ex);
            }

            if(mensaje instanceof EstadoJuego){
                EstadoJuego estado = (EstadoJuego) mensaje;
                //Compruebo si ha ganado o se sigue jugando
                if(estado.getEstadoJuego().equals(Estado.FIN_PARTIDA)){
                    //enviarClasificacion(agente, CFP_KEY);
                }
            }else{
                System.err.println("agentes.agenteTableroConecta4.jugarPartidaIniciador.handleInform(): ERROR EN EL ESTADO DE JUEGO");
            }
            
        }
    }
      
    /**
     * Tarea que prepara la partida nueva 
     */
    public class TareaPrepararPartida extends OneShotBehaviour {


        @Override
        public void action() {
            if(partidaJugada){
                if(primeraPintada){
                    addBehaviour(new pintar(myAgent, 1000));
                    primeraPintada = false;
                }
                File archivo = null;
                FileReader fr = null;
                BufferedReader br = null;
                boolean centinela = true;
                archivo = new File (idPartida+"-"+partidaActual+".txt");
                if(archivo.exists()){
                    System.out.println("LEO PARTTIDA NUEVA");

                    try {
                       // Apertura del fichero y creacion de BufferedReader para poder
                       // hacer una lectura comoda (disponer del metodo readLine()).

                       partidaActual++;
                       fr = new FileReader (archivo);
                       br = new BufferedReader(fr);

                       // Lectura del fichero
                       String linea;
                       String jugActivo = br.readLine();
                       while((linea=br.readLine())!=null){
                           if(linea.equals("jug1") || linea.equals("jug2")){
                               if(linea.equals("jug1")){
                                    ArrayList listaPuntos = new ArrayList();
                                    listaPuntos.add(0, VICTORIA.getValor());
                                    listaPuntos.add(1, DERROTA.getValor());
                                    enviarclasificacion = true;
                                    centinela = false;
                                    addBehaviour(new EnviarClas(agenteTableroBarquitos.this, 5000, listaPuntos, (ArrayList) jugadores));
                               }else{
                                    ArrayList listaPuntos = new ArrayList();
                                    ArrayList jug = new ArrayList();
                                    listaPuntos.add(0, VICTORIA.getValor()); 
                                    listaPuntos.add(1, DERROTA.getValor());            
                                    jug.add(0, jugadores.get(1));
                                    jug.add(1, jugadores.get(0));   
                                    enviarclasificacion = true;
                                    centinela = false;
                                    addBehaviour(new EnviarClas(agenteTableroBarquitos.this, 5000, listaPuntos, jug));
                               }                               
                           }else{
                               String [] campos = linea.split(",");
                                if(campos[3].equals("jug1")){                      
                                    ventanaTablero2.getTablero().colocarFicha(Integer.parseInt(campos[1]), Integer.parseInt(campos[0]), convertirStringAColor(campos[2]));
                                    jugActivo = "jug2";
                                }else{                                
                                    ventanaTablero1.getTablero().colocarFicha(Integer.parseInt(campos[1]), Integer.parseInt(campos[0]), convertirStringAColor(campos[2]));
                                    jugActivo = "jug1";
                                }
                           }

                       }
                       if(centinela){                         
                           fr.close();                           
                           addBehaviour(new reiniciarPartida(agenteTableroBarquitos.this, 5000));                                                  
                       }
                    }
                    catch(Exception e){
                       e.printStackTrace();
                    }finally{
                        // En el finally cerramos el fichero, para asegurarnos
                        // que se cierra tanto si todo va bien como si salta 
                        // una excepcion.
                        try{                    
                           if( null != fr ){   
                              fr.close();     
                           }                  
                        }catch (Exception e2){ 
                           e2.printStackTrace();
                        }
                    }
                }
            }else{
                try {
                    fichero = new FileWriter(idPartida+"-"+partidaActual+".txt");
                    } catch (IOException ex) {
                        Logger.getLogger(agenteTableroBarquitos.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    pw = new PrintWriter(fichero);
                ACLMessage msg = new ACLMessage(ACLMessage.PROPOSE);

                for(int i = 0; i < jugadores.size(); i++){
                    msg.addReceiver(jugadores.get(i).getAgenteJugador());
                }

                msg.setProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE);
                ColocarBarcos object = new ColocarBarcos(new Juego(idPartida, minVictorias, modoJuego, Vocabulario.TipoJuego.BARCOS));
                Action a = new Action(myAgent.getAID(), object);
                try {
                    agenteTableroBarquitos.this.mensajeFillContent(msg, a, OntologiaJuegoBarcos.getInstance());
                } catch (BeanOntologyException ex) {
                    Logger.getLogger(agenteTableroBarquitos.class.getName()).log(Level.SEVERE, null, ex);
                } catch (AgenteOntologiaJuegos.OntologiaDesconocida ex) {
                    Logger.getLogger(agenteTableroBarquitos.class.getName()).log(Level.SEVERE, null, ex);
                } catch (Codec.CodecException ex) {
                    Logger.getLogger(agenteTableroBarquitos.class.getName()).log(Level.SEVERE, null, ex);
                } catch (OntologyException ex) {
                    Logger.getLogger(agenteTableroBarquitos.class.getName()).log(Level.SEVERE, null, ex);
                }
                pw.println(jugadorActivo);
                addBehaviour(new preparacionPartidaIniciador(myAgent, msg));
            }

        }
    }
        
    /**
     * Tarea que envia la clasificacion de la partida una vez se haya terminado de representar gráficamente
     * @param puntuacionLista
     * @throws BeanOntologyException
     * @throws agentes.AgenteOntologiaJuegos.OntologiaDesconocida
     * @throws jade.content.lang.Codec.CodecException
     * @throws OntologyException 
     */  
    void enviarClasificacion(ArrayList puntuacionLista, ArrayList jug) throws BeanOntologyException, AgenteOntologiaJuegos.OntologiaDesconocida, Codec.CodecException, OntologyException {

        ACLMessage mensaje = new ACLMessage(ACLMessage.INFORM);
        mensaje.setSender(getAID());
        mensaje.addReceiver(agenteGrupo);
        Juego juego = new Juego(idPartida,minVictorias, modoJuego, Vocabulario.TipoJuego.BARCOS);

        ClasificacionJuego clasificacion = new ClasificacionJuego(juego,new jade.util.leap.ArrayList((ArrayList) jug),new jade.util.leap.ArrayList((ArrayList) puntuacionLista));
        //Añadir La Ontologia necesaria
        agenteTableroBarquitos.this.mensajeFillContent(mensaje, (ContentElement) clasificacion, OntologiaJuegoBarcos.getInstance());
        send(mensaje);
        ventanaTablero1.dispose();
        ventanaTablero2.dispose();
        doDelete();
    }
      
    /**
     * Tarea que comprueba el movimiento
     * @param fila
     * @param columna
     * @param tab
     * @return 
     */
    public boolean comprobarMovimiento(int fila,int columna, int[][] tab){
        boolean parada = false;
        boolean parada1 = false;
        boolean p1 = false;
        boolean p2 = false;
        int n = 0;
        int a1 = fila;
        int a2 = fila;
        int b1 = columna;
        int b2 = columna;
        //0-No visitado, 1-Agua, 2-Tocado, 3-Barco inicial
        if(fila-1 >= 0)
            if(tab[fila-1][columna] == 2)
                n = 1;
        if(fila+1 < tamanoTableroX)
            if(tab[fila+1][columna] == 2)
                n = 1;
        if(columna-1 >= 0)
            if(tab[fila][columna-1] == 2)
                n = 2;
        if(columna+1 < tamanoTableroY)
            if(tab[fila][columna+1] == 2)
                n = 2;
        
        if(n == 0){
            return false;
        }else{
            if(n == 1){                
                while(!parada && !parada1){
                    if(tab[a1][columna] == 2){
                        if(a1-1 >= 0){
                            a1--;
                        }else{
                            p1 = true;
                            parada = true;
                        }
                    }else{                   
                        if(tab[a1][columna] != 3){
                            p1 = true;
                        }
                        parada = true;
                    }
                                           
                    if(tab[a2][columna] == 2){
                        if(a2+1 < tamanoTableroX){
                            a2++;
                        }else p2 = true;
                    }else{
                        if(tab[a2][columna] != 3){
                            p2 = true;
                        }
                    }                
                }
            }
            
            if(n == 2){                
                while(!parada && !parada1){
                    if(tab[fila][b1] == 2){
                        if(b1-1 >= 0){
                            b1--;
                        }else{
                            p1 = true;
                            parada = true;
                        }
                    }else{                   
                        if(tab[fila][b1] != 3){
                            p1 = true;
                        }
                        parada = true;
                    }
                                           
                    if(tab[fila][b2] == 2){
                        if(b2+1 < tamanoTableroX){
                            b2++;
                        }else p2 = true;
                    }else{
                        if(tab[fila][b2] != 3){
                            p2 = true;
                        }
                    }                
                }
            }
        }
        return p1 && p2;
    }
    
    /**
     * Tarea que comprueba si ha habido victoria o no
     * @throws agentes.AgenteOntologiaJuegos.OntologiaDesconocida
     * @throws jade.content.lang.Codec.CodecException
     * @throws OntologyException 
     */
    public void comprobarVictoria() throws AgenteOntologiaJuegos.OntologiaDesconocida, Codec.CodecException, OntologyException{
        int jugador1 = 0;
        int jugador2 = 0;
        boolean ganar = false;
        for(int i = 0; i < victorias.size(); i++){
            if(victorias.get(i) == 1)
                jugador1++;
            else {
                if(victorias.get(i) == 2)
                    jugador2++;
            }
            
        }
        if(jugador1 >= minVictorias ){
            ArrayList listaPuntos = new ArrayList();
            listaPuntos.add(0, VICTORIA.getValor());
            listaPuntos.add(1, DERROTA.getValor());
            enviarclasificacion = true;
            pw.println("jug1");
            addBehaviour(new EnviarClas(this, 5000, listaPuntos, (ArrayList) jugadores));
            ganar = true;
        }
        if(jugador2 >= minVictorias ){
            ArrayList listaPuntos = new ArrayList();
            ArrayList jug = new ArrayList();
            listaPuntos.add(0, VICTORIA.getValor()); 
            listaPuntos.add(1, DERROTA.getValor());            
            jug.add(0, jugadores.get(1));
            jug.add(1, jugadores.get(0));         
            enviarclasificacion = true;
            pw.println("jug2");
            addBehaviour(new EnviarClas(this, 5000, listaPuntos,jug));
            ganar = true;
            
        }
        
        if(!ganar){
            resetear = true;
            addBehaviour(new comprobarPintura(this, 5000));
        }
    }
    
    /**
     * Tarea que resetea el tablero cuando son las partidas al mejor de 3 o de 5
     */
    public void resetearTablero(){
        ventanaTablero1.dispose();
        ventanaTablero2.dispose();
        ventanaTablero1 = new MainFrameBarquitos("Jugador1");
        ventanaTablero2 = new MainFrameBarquitos("Jugador2");
        for(int i = 0; i < tamanoTableroX; i ++){
            for(int j = 0; j < tamanoTableroY; j ++){
                Tablero1[i][j] = 0;
                ventanaTablero1.getTablero().colocarFicha(i, 
                                j, VERDE);
                ventanaTablero1.getTablero().colorearFicha();//VERDE para identificar que no hay barco inicialmente ni ha sido visitado
            }
        }
        ventanaTablero1.setVisible(true);
        
        for(int i = 0; i < tamanoTableroX; i ++){
            for(int j = 0; j < tamanoTableroY; j ++){
                Tablero2[i][j] = 0;
                ventanaTablero2.getTablero().colocarFicha(i, 
                                j, VERDE);
                ventanaTablero2.getTablero().colorearFicha();//VERDE para identificar que no hay barco inicialmente ni ha sido visitado
            }
        }
        ventanaTablero2.setVisible(true);
        primeraPasada = true;
        partidaActual++;
        
        if(!partidaJugada)
            try {
                fichero.close();
        } catch (IOException ex) {
            Logger.getLogger(agenteTableroBarquitos.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        addBehaviour(new TareaPrepararPartida());      
    }
    
    /**
     * Tarea que nos sirve de control, es decir, hasta que no acabe de representarse una partida
     * no permite que se resetee el tablero
     */
    public class comprobarPintura extends TickerBehaviour{

        public comprobarPintura(Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
            colaJugador1 = ventanaTablero1.getTablero().getCola().size();
            colaJugador2 = ventanaTablero2.getTablero().getCola().size(); 
            if(resetear && colaJugador1 == 0 && colaJugador2 == 0){
                resetear = false;
                resetearTablero();
               
            }               
        }       
    }
    
    /**
     * Tarea de control que hasta que no se acaba de pintar la partida no envía
     * la clasificación
     */
    public class EnviarClas extends TickerBehaviour{

        private ArrayList listapuntos;
        private ArrayList jug;
        
        public EnviarClas(Agent a, long period, ArrayList lista, ArrayList jug) {
            super(a, period);
            listapuntos = lista;
            this.jug = jug;
            
            
        }

        @Override
        protected void onTick() {
            colaJugador1 = ventanaTablero1.getTablero().getCola().size();
            colaJugador2 = ventanaTablero2.getTablero().getCola().size(); 
            if(colaJugador1 == 0 && colaJugador2 == 0 && enviarclasificacion){
                enviarclasificacion = false;
                try {
                    enviarClasificacion(listapuntos,jug);
                } catch (AgenteOntologiaJuegos.OntologiaDesconocida ex) {
                    Logger.getLogger(agenteTableroBarquitos.class.getName()).log(Level.SEVERE, null, ex);
                } catch (Codec.CodecException ex) {
                    Logger.getLogger(agenteTableroBarquitos.class.getName()).log(Level.SEVERE, null, ex);
                } catch (OntologyException ex) {
                    Logger.getLogger(agenteTableroBarquitos.class.getName()).log(Level.SEVERE, null, ex);
                }
                
                try {
                    if(!partidaJugada)
                        fichero.close();
                } catch (IOException ex) {
                    Logger.getLogger(agenteTableroBarquitos.class.getName()).log(Level.SEVERE, null, ex);
                }
            }              
        }      
    }
    
    /**
     * Tarea de control que reinicia la partida cuando ya se haya termiando de 
     * pintar el juego, lo utiizamos a la hora de la reproduccion de las partidas
     */
    public class reiniciarPartida extends TickerBehaviour{
        
        public reiniciarPartida(Agent a, long period) {
            super(a, period);           
        }

        @Override
        protected void onTick() {
            colaJugador1 = ventanaTablero1.getTablero().getCola().size();
            colaJugador2 = ventanaTablero2.getTablero().getCola().size(); 
            if(colaJugador1 == 0 && colaJugador2 == 0){
                ventanaTablero1.dispose();
                ventanaTablero2.dispose();
                ventanaTablero1 = new MainFrameBarquitos("Jugador1");
                ventanaTablero2 = new MainFrameBarquitos("Jugador2");
                for(int i = 0; i < tamanoTableroX; i ++){
                    for(int j = 0; j < tamanoTableroY; j ++){
                        Tablero1[i][j] = 0;
                        ventanaTablero1.getTablero().colocarFicha(i, 
                                        j, VERDE);
                        ventanaTablero1.getTablero().colorearFicha();//VERDE para identificar que no hay barco inicialmente ni ha sido visitado
                    }
                }
                ventanaTablero1.setVisible(true);

                for(int i = 0; i < tamanoTableroX; i ++){
                    for(int j = 0; j < tamanoTableroY; j ++){
                        Tablero2[i][j] = 0;
                        ventanaTablero2.getTablero().colocarFicha(i, 
                                        j, VERDE);
                        ventanaTablero2.getTablero().colorearFicha();//VERDE para identificar que no hay barco inicialmente ni ha sido visitado
                    }
                }
                ventanaTablero2.setVisible(true);
                addBehaviour(new TareaPrepararPartida());              
            }               
        }        
    }
    
    /**
     * Función para transformar String en Color
     * @param s
     * @return 
     */
    Vocabulario.Color convertirStringAColor(String s){
        if(s.equals("ROJO"))
            return ROJO;
        if(s.equals("AMARILLO"))
            return AMARILLO;
        if(s.equals("AZUL"))
            return AZUL;
        if(s.equals("VERDE"))
            return VERDE;
        return null;
    }    
}

