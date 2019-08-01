
package agentes;

import GUI.MainFrameConecta4;
import jade.*;
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
import jade.lang.acl.MessageTemplate;
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
import static java.lang.Thread.sleep;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import juegosTablero.Vocabulario;
import juegosTablero.Vocabulario.Estado;
import juegosTablero.Vocabulario.ModoJuego;
import juegosTablero.Vocabulario.Puntuacion;
import juegosTablero.aplicacion.OntologiaJuegoBarcos;
import juegosTablero.aplicacion.OntologiaJuegoConecta4;
import juegosTablero.aplicacion.conecta4.EstadoJuego;
import juegosTablero.aplicacion.conecta4.MovimientoEntregado;
import java.util.ArrayList;
import static juegosTablero.Vocabulario.Color.AMARILLO;
import static juegosTablero.Vocabulario.Color.AZUL;
import static juegosTablero.Vocabulario.Color.ROJO;
import static juegosTablero.Vocabulario.Color.VERDE;
import static juegosTablero.Vocabulario.Estado.FIN_PARTIDA;
import static juegosTablero.Vocabulario.Estado.GANADOR;
import static juegosTablero.Vocabulario.Estado.SEGUIR_JUGANDO;
import static juegosTablero.Vocabulario.Puntuacion.DERROTA;
import static juegosTablero.Vocabulario.Puntuacion.EMPATE;
import static juegosTablero.Vocabulario.Puntuacion.VICTORIA;
import juegosTablero.aplicacion.barcos.ColocarBarcos;
import juegosTablero.aplicacion.conecta4.Ficha;
import juegosTablero.aplicacion.conecta4.Movimiento;
import juegosTablero.dominio.elementos.ClasificacionJuego;
import juegosTablero.dominio.elementos.Juego;
import juegosTablero.dominio.elementos.Jugador;
import juegosTablero.dominio.elementos.Motivacion;
import juegosTablero.dominio.elementos.PedirMovimiento;
import juegosTablero.dominio.elementos.Posicion;
import util.ArrayConversor;

/**
 *
 * @author Julian
 */
public class agenteTableroConecta4 extends AgenteOntologiaJuegos{

    private List<Jugador> jugadores;                ///> Lista de Jugadores que están participando en la partida
    private List puntuacion;                        ///> Lista de Puntuación que corresponde en índice con el indice de jugadores
    private ModoJuego modoJuego;                    ///> Indica el Modo de Juego (ÚNICO o TORNEO)
    private MainFrameConecta4 ventanaTablero;       ///> Se encarga de crear el Tablero visual
    private Queue<Movimiento> pintarMovimientos;    ///> Cola que se encargar de pintar los movimientos conforme van llegando
    private ArrayConversor<Jugador> conversor;      ///> Clase usada para convertir ArrayList de jade a java
    private FileWriter fichero;                     ///> Se encarga de crear el fichero para las partidas guardadas
    private PrintWriter pw;                         ///> Escribe en el fichero creado
    private String jugadorActivo;                   ///> Indica cual es el jugador activo para indicar luego quien es el siguiente
    private String idPartida;                       ///> Indica el idPartida que se está jugando
    private int victoriasJug1, victoriasJug2;       ///> Contabiliza las partidas que llevan ganadas el jugador 1 y 2
    private int partidaActual;                      ///> Cuando hay mas partidas para lograr una victoria las va contabilizando
    private int minVictorias;                       ///> Mínimo de victorias para finalizar una partida
    private AID agenteGrupo;                        ///> AID del agente Grupo
    private boolean reiniciar;                      ///> Variable booleana que indica cuando hay que reiniciar el tablero
    private boolean finPartida;                     ///> Variable booleana que indica el final de la partida
    private boolean partidaJugada;                  ///> Variable booleana que indica si existe dicha partida si ha sido jugada
    private boolean centinela;                      ///> Varibale booleana que indica si ha acabado de leer la partida
    
    
    @Override
    protected void setup() {
        super.setup();
        //Leo los argumentos que me llegan del AgenetGrupo
        Object[] objetos = this.getArguments();
        conversor=new ArrayConversor<>();
        jugadores = conversor.fromJade2Java((jade.util.leap.ArrayList) objetos[0], Jugador.class);
        minVictorias = (int) objetos[1];
        modoJuego = (ModoJuego) objetos[2];
        idPartida = (String)objetos[3];
        agenteGrupo = (AID) objetos[4];
        
        //Configuración del GUI
        ventanaTablero = new MainFrameConecta4(idPartida);
        ventanaTablero.setVisible(true);

        
        //Inicializar variables
        pintarMovimientos = new LinkedList<>();
        reiniciar = false;
        finPartida = false;
        victoriasJug1 = 0;
        victoriasJug2 = 0;
        partidaJugada = false;
        partidaActual = 0;
        centinela = false;
        jugadorActivo = "Jug2";
        puntuacion = new ArrayList();
        puntuacion.add(1); puntuacion.add(1);
        
        //Inicio las Tareas
        addBehaviour(new TareaPrepararPartida());
        addBehaviour(new TareaPintar(this, 500));

    }
    
    /**
     * Finaliza la ejecución del agente
     */
    @Override 
    protected void takeDown(){
        super.takeDown();
        //Desregristo del agente de las Páginas Amarillas
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
        }
        
        //Se liberan los recursos y se despide
        System.out.println("Finaliza la ejecución de " + this.getName());
    }
    
    /**
     * Defino la Ontología que va a utilizar mi agente Jugador
     * @return vector con las Ontologías que usa el agente
     * @throws Exception Si la Ontología no ha sido encontrada
     */
    @Override
    public Ontology [] defineOntology() throws Exception{
        Ontology[] ret = {
            OntologiaJuegoConecta4.getInstance()
        };
        return ret;
    }
    
    /**
     * Se registra en las páginas amarillas
     * @return Variable que representa al juego del Conecta4
     */
    @Override
    public String nombreRegistro(){
        return Vocabulario.NombreServicio.JUEGO_CONECTA_4.toString();
    }
    

    /**
     * Clase que se encargará de pintar los movimientos sacándolos de la colaS
     */
     public class TareaPintar extends TickerBehaviour {

        public TareaPintar(Agent a, long period) {
            super(a, period);
        }


        @Override
        protected void onTick() {
            //Si la cola no está vacía pinta el tablero
            if(!pintarMovimientos.isEmpty()){
                Movimiento m = pintarMovimientos.poll();
                int x = m.getPosicion().getCoorX();
                int y = m.getPosicion().getCoorY();
                //Si la partida no es jugada, se escribe en el archivo creado
                if(!partidaJugada){
                    if(m.getFicha().getColor().equals(ROJO)){
                        pw.println(x+","+y+",ROJO,jug1");
                    }else{
                        pw.println(x+","+y+",AZUL,jug2");
                    }  
                }
                
                //Pinto el tablero
                ventanaTablero.getTablero().colocarFicha(x, y, m.getFicha().getColor());
            
            //Si esta vacía la partida ha acabado
            }else{
                if(finPartida){
                    try {
                        finPartida = false;
                        enviarClasificacion(puntuacion);
                    } catch (Exception ex) {
                        System.err.println("agentes.agenteTableroConecta4.TareaPintar.onTick(): ERROR AL ENVIAR CLASIFICACIÓN");;
                    } 
                }
                //Si todavia no se ha llegado al minVictorias se reinicia el tablero
                if(partidaJugada){
                    if(centinela && minVictorias != 1){    
                        addBehaviour(new TareaReiniciarTablero(myAgent, 5000));             
                                                  
                    }
                }else{
                    if(minVictorias != victoriasJug1 || minVictorias != victoriasJug2){
                        addBehaviour(new TareaReiniciarTablero(myAgent, 5000)); 
                    }
                }
                
            }
        }
    }
    

    //JUGAR PARTIDA
    /**Para jugar una partida se comunicará el AgenteTablero con los 
     * AgenteJugador que están implicados. Para ello utilizaremos el protocolo 
     * ContractNet para implementar la comunicación.*/
     
     private class TareaJugarPartida extends ContractNetInitiator {

        /**
         * Constructor parametrizado de la clase
         *
         * @param agente Agente que llama al protocolo
         * @param plantilla Mensaje CFP
         */
        public TareaJugarPartida(Agent agente, ACLMessage msg) {
            super(agente, msg);
        }
        
        /**
         * Compruebo cual de los dos mensajes que me han llegado son Movimientos
         * @param m1 Mensaje 1
         * @param m2 Mensaje 2
         * @return Devuelve el MovimientoEntregado
         * @throws InterruptedException 
         */
        public MovimientoEntregado comprobarMovimiento(ContentElement m1, ContentElement m2) throws InterruptedException{

            MovimientoEntregado mov = null;
        
            if(m1 instanceof MovimientoEntregado){
                 mov = (MovimientoEntregado) m1;
                 pintarMovimientos.add(new Movimiento(mov.getMovimiento().getFicha(), mov.getMovimiento().getPosicion()));
             }
            
            if(m2 instanceof MovimientoEntregado){
                 mov = (MovimientoEntregado) m2;
                 pintarMovimientos.add(new Movimiento(mov.getMovimiento().getFicha(), mov.getMovimiento().getPosicion()));
             }
            
            return mov;
        }
    
        /**
         * Método que gestiona los mensajes que llegan de Agente Jugador
         * @param responses
         * @param acceptances 
         */
        @Override
        protected void handleAllResponses(Vector responses, Vector acceptances){
            ContentElement mensaje1 = null;
            ContentElement mensaje2 = null;
            MovimientoEntregado m = null;
            //Extraigo los mensajes de 2 en 2
            for(Iterator it=responses.iterator(); it.hasNext();){
                try {
                    mensaje1 = agenteTableroConecta4.this.extraerMensaje((ACLMessage) it.next());
                    mensaje2 = agenteTableroConecta4.this.extraerMensaje((ACLMessage) it.next());
                } catch (Exception ex) {
                    System.err.println("agentes.agenteTableroConecta4.jugarPartidaIniciador.handleAllResponses(): ERROR AL EXTRAER EL MENSAJE");
                }
            }

            //Compruebo cual de ellos es un movimiento y lo asigno
            try {
                m = comprobarMovimiento(mensaje1, mensaje2);
            } catch (InterruptedException ex) {
                System.err.println("agentes.agenteTableroConecta4.jugarPartidaIniciador.handleAllResponses(): ERROR AL COMPROBAR MOVIMIENTO");
            }
            
            //Creo mensajes de replica para los dos casos y los añado al vector de acceptances
            for(Iterator it= responses.iterator(); it.hasNext();){
                ACLMessage replica = ((ACLMessage) it.next()).createReply();
                replica.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                try {
                    replica = agenteTableroConecta4.this.mensajeFillContent(replica, m, OntologiaJuegoConecta4.getInstance());
                } catch (Exception ex) {
                    System.err.println("ERROR");
                }
                acceptances.addElement(replica);                  
            }
           
        }
       

        /**
         * Manejador de rechazos de proposiciones.
         * @param rechazo El agente abandona
         */
        @Override
        protected void handleRefuse(ACLMessage rechazo) {
            //Hay abandono, por lo que enviamos la Clasificación al AgenteGrupoJuegos 
          /*  if(rechazo.getSender().equals(jug2.getAgenteJugador())){
                
            }*/
           System.out.println("RECHAZADO");
            
            //Como organizo la lista de puntuaciones
            //enviarClasificacion(agente, CFP_KEY);
       }
    
        /**
         * Manejador de los mensajes inform
         * @param inform Mensaje recibido
         */
        @Override
        protected void handleInform(ACLMessage inform){
            ContentElement mensaje = null;
            try {
                mensaje = agenteTableroConecta4.this.extraerMensaje(inform);
            } catch (Exception ex) {
                Logger.getLogger(agenteTableroConecta4.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            //Si el mensaje extraido es EstadoJuego realizo las siguientes comprobaciones
            if(mensaje instanceof EstadoJuego){
                EstadoJuego estado = (EstadoJuego) mensaje;
                //Compruebo si ha ganado o se sigue jugando
                if(estado.getEstadoJuego().equals(Estado.GANADOR)){
                    //Si el emisor del mensajes corresponde con el jugador de índice 0, es el que gana
                    if(inform.getSender().equals(jugadores.get(0).getAgenteJugador())){
                        victoriasJug1++;
                        puntuacion.set(0, VICTORIA.getValor());
                        puntuacion.set(1, DERROTA.getValor());
                    //Si no, gana el jugador de índice 1
                    }else{
                        victoriasJug2++;
                        puntuacion.set(1, VICTORIA.getValor());
                        puntuacion.set(0, DERROTA.getValor());
                    }

                    //Si se ha llegado al minVictorias es el fin de la partida
                    if(minVictorias == victoriasJug1 || minVictorias == victoriasJug2){
                        finPartida = true; 
                    //Si no se reinicia el tablero para jugar otra partidas
                    }else{
                        reiniciar = true;
                        addBehaviour(new TareaReiniciarTablero(myAgent, 1000));
                    }

                //Si el estado es SEGUIR_JUGANDO vuelve a pedir un movimiento al siguiente jugador
                }else if(estado.getEstadoJuego().equals(SEGUIR_JUGANDO)){
                    try {
                        reset(pedirMovimiento());
                    } catch (Exception ex) {
                        System.out.println("agentes.agenteTableroConecta4.TareaJugarPartida.handleInform(): ERROR AL PEDIR EL MOVIMIENTO");
                    }
                }
                //Si el estado es FIN_PARTIDA es que el resultado es empate
                else if(estado.getEstadoJuego().equals(FIN_PARTIDA)){
                    if(minVictorias != victoriasJug1 || minVictorias != victoriasJug2){
                        reiniciar = true;
                        addBehaviour(new TareaReiniciarTablero(myAgent, 1000));
                    }else{
                        puntuacion.set(0, EMPATE.getValor());
                        puntuacion.set(1, EMPATE.getValor());
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
                mensaje = agenteTableroConecta4.this.extraerMensaje(failure);
            } catch (Exception ex) {
                Logger.getLogger(agenteTableroConecta4.class.getName()).log(Level.SEVERE, null, ex);
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
    * Se encarga de gestionar el turno y de pedir el movimiento al jugador que corresponda
    * @return
    * @throws InterruptedException 
    */
    public ACLMessage pedirMovimiento() throws InterruptedException {
        //Elegimos al jugador activo que cambia por turnos
        Jugador jug = null;
        if(jugadorActivo.equals("Jug1")){
            jugadorActivo = "Jug2";
            jug = jugadores.get(1);
        }else{
            jugadorActivo = "Jug1";
            jug = jugadores.get(0);
        }
        
        //Creamos el mensaje CFP
        ACLMessage msg = new ACLMessage(ACLMessage.CFP);
        for(int i = 0; i < jugadores.size(); i++){
            msg.addReceiver(jugadores.get(i).getAgenteJugador());
        }
	msg.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
        PedirMovimiento object = new PedirMovimiento(new Juego(idPartida, minVictorias, modoJuego, Vocabulario.TipoJuego.CONECTA_4), jug);
        Action accion = new Action(getAID(), object);

        //Rellenamos el mensaje con PedirMovimiento
        try {
            agenteTableroConecta4.this.mensajeFillContent(msg, accion, OntologiaJuegoConecta4.getInstance());
        } catch (Exception ex) {
            System.err.println("agentes.agenteTableroConecta4.informarMovimiento(): ERROR AL RELLENAR MENSAJE ");
        }
        
        return msg;
        
    }
    
  
    
    //Enviamos un mensaje INFORM para informar de la clasificación al Agente Grupo
    void enviarClasificacion(List puntuacionLista) throws BeanOntologyException, AgenteOntologiaJuegos.OntologiaDesconocida, Codec.CodecException, OntologyException, InterruptedException {

            ACLMessage mensaje = new ACLMessage(ACLMessage.INFORM);
            mensaje.setSender(getAID());
            mensaje.addReceiver(agenteGrupo);
            
            //Añadimos el ganador de la partida
            if(!partidaJugada){
               if(puntuacionLista.get(0).equals(VICTORIA.getValor())){
                pw.println("jug1");
                }else if(puntuacionLista.get(1).equals(VICTORIA.getValor())){
                    pw.println("jug2");
                }else{
                    pw.println("emp");
                }
            }
            
            Juego juego = new Juego(idPartida,minVictorias, modoJuego, Vocabulario.TipoJuego.CONECTA_4);
            ClasificacionJuego clasificacion = new ClasificacionJuego(juego,new jade.util.leap.ArrayList((ArrayList) jugadores),new jade.util.leap.ArrayList((ArrayList) puntuacionLista));
           
            //Añadir La Ontologia necesaria
            mensajeFillContent(mensaje, (ContentElement) clasificacion, OntologiaJuegoConecta4.getInstance());
            send(mensaje);
            
            //UNA VEZ ENVIADA LA CLASIFICACIÓN, DEBO BORRAR EL AGENTE TABLERO
            TimeUnit.SECONDS.sleep(5);
            ventanaTablero.dispose();
            if(!partidaJugada){
                pw.close();
            }
            doDelete();
        
    }
    
    /**
     * Tarea que se encarga de reiniciar el Tablero para jugar otra ronda
     */
    public class TareaReiniciarTablero extends TickerBehaviour{

        public TareaReiniciarTablero(Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
            
            if(reiniciar && pintarMovimientos.isEmpty()){
                reiniciar = false;
                if(!partidaJugada){
                    try {
                        fichero.close();
                    } catch (IOException ex) {
                        System.err.println("agentes.agenteTableroConecta4.TareaReiniciarTablero.onTick(): ERROR AL CERRAR EL FICHERO");
                    }
                    ventanaTablero.dispose();
                    ventanaTablero = new MainFrameConecta4(idPartida+"_"+partidaActual);
                    ventanaTablero.setVisible(true);
                    addBehaviour(new TareaPrepararPartida());
                }else{
                    //Compruebo que la partida no existe
                    File archivo;
                    archivo = new File (idPartida+"-"+partidaActual+".txt");
                    //Comprobamos si el archivo existe para saber si hemos jugado dicha partida
                    if(archivo.exists()){
                        ventanaTablero.dispose();
                        ventanaTablero = new MainFrameConecta4(idPartida+"_"+partidaActual);
                        ventanaTablero.setVisible(true);
                        addBehaviour(new TareaPrepararPartida());
                    }
                }
                    
            }
        }   
        
    }
    
    /**
     * Tarea que gestionara la creación y lectura de ficheros en función de si la
     * partida se ha jugado ya o todavía no en función del identificador
     */
    public class TareaPrepararPartida extends OneShotBehaviour {

        File archivo;
        FileReader fr;
        BufferedReader br;
    
        @Override
        public void action() {
            archivo = new File (idPartida+"-"+partidaActual+".txt");
            //Comprobamos si el archivo existe para saber si hemos jugado dicha partida
            if(archivo.exists()){
                partidaJugada = true;
            }else{
                partidaJugada = false;
            }

            //Si la partida la hemos jugado, se leera el fichero de la partida
            if(partidaJugada){
                    try {
                       partidaActual++;
                       fr = new FileReader (archivo);
                       br = new BufferedReader(fr);

                       // Lectura del fichero
                       String linea;
                       //La última linea del último fichero indica quien es el ganador o resultado de la partida
                       //Según como sea esta línea se realizará un resultado u otro
                       while((linea=br.readLine())!=null){
                           if(linea.equals("jug1") || linea.equals("jug2") || linea.equals("emp")){
                               if(linea.equals("jug1")){
                                    puntuacion.add(0, VICTORIA.getValor());
                                    puntuacion.add(1, DERROTA.getValor());
                                    victoriasJug1 = minVictorias;
                                    centinela = false;
                                    finPartida = true;
                               }else if(linea.equals("jug2")){
                                    puntuacion.add(0, DERROTA.getValor());
                                    puntuacion.add(1, VICTORIA.getValor());
                                    victoriasJug2 = minVictorias;
                                    centinela = false;
                                    finPartida = true;
                               }else{
                                    puntuacion.add(0, EMPATE.getValor());
                                    puntuacion.add(1, EMPATE.getValor());
                                    centinela = false;
                                    finPartida = true;
                               }                               
                           }else{
                                //Almacena las filas y las añade a la cola de pintarMovimientos para que se encargue de pintarlos
                                String[] campos = linea.split(",");
                                if(campos[2].equals("ROJO")){
                                     pintarMovimientos.add(new Movimiento(new Ficha(ROJO),
                                                        new Posicion(Integer.parseInt(campos[0]), Integer.parseInt(campos[1]))));
                                }else{
                                    pintarMovimientos.add(new Movimiento(new Ficha(AZUL),
                                                        new Posicion(Integer.parseInt(campos[0]), Integer.parseInt(campos[1]))));
                                }
                                centinela = true;
                           }
                       }

                       reiniciar = true;                           

                    }
                    catch(Exception e){
                       e.printStackTrace();
                    }finally{
                        // En el finally cerramos el fichero, para asegurarnos
                        try{                    
                           if(fr != null){   
                              fr.close();     
                           }                  
                        }catch (Exception e){ 
                            System.err.println("agentes.agenteTableroConecta4.TareaPrepararPartida.action(): ERROR AL CERRAR EL FICHERO");
                        }
                    }
            //Si la partida no ha sido jugada se crea el fichero para que se pueda escribir los movimientos
            //de la partida que se vaya a jugar
            }else{
                try {
                    fichero = new FileWriter(idPartida+"-"+partidaActual+".txt");
                    } catch (IOException ex) {
                       System.err.println("agentes.agenteTableroConecta4.TareaPrepararPartida.action():ERROR AL CREAR LA PARTIDA");
                    }
                    partidaActual++;
                    pw = new PrintWriter(fichero);

                try {
                    addBehaviour(new TareaJugarPartida(myAgent, pedirMovimiento()));
                } catch (InterruptedException ex) {
                    System.err.println("agentes.agenteTableroConecta4.TareaPrepararPartida.action(): ERROR AL INICIAR PARTIDA");
                }
            }
        }
    }
}

