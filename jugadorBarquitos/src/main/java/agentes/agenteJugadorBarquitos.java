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

import jade.content.ContentElement;
import jade.content.ContentManager;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.BeanOntologyException;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import static jade.domain.FIPANames.InteractionProtocol.FIPA_CONTRACT_NET;
import static jade.domain.FIPANames.InteractionProtocol.FIPA_PROPOSE;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.ContractNetResponder;
import jade.proto.ProposeResponder;
import jade.util.leap.List;
import java.util.ArrayList;
import static java.util.Collections.list;
import java.util.HashMap;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import juegosTablero.Vocabulario;
import juegosTablero.Vocabulario.Efecto;
import juegosTablero.Vocabulario.Estado;
import static juegosTablero.Vocabulario.Estado.ABANDONO;
import static juegosTablero.Vocabulario.Estado.FIN_PARTIDA;
import static juegosTablero.Vocabulario.Estado.GANADOR;
import static juegosTablero.Vocabulario.Estado.SEGUIR_JUGANDO;
import juegosTablero.Vocabulario.Motivo;
import juegosTablero.Vocabulario.NombreServicio;
import juegosTablero.Vocabulario.Orientacion;
import static juegosTablero.Vocabulario.Orientacion.HORIZONTAL;
import static juegosTablero.Vocabulario.Orientacion.VERTICAL;
import static juegosTablero.Vocabulario.TIPO_SERVICIO;
import juegosTablero.Vocabulario.TipoBarco;
import static juegosTablero.Vocabulario.TipoBarco.ACORAZADO;
import static juegosTablero.Vocabulario.TipoBarco.DESTRUCTOR;
import static juegosTablero.Vocabulario.TipoBarco.FRAGATA;
import static juegosTablero.Vocabulario.TipoBarco.PORTAAVIONES;
import static juegosTablero.Vocabulario.TipoJuego.BARCOS;
import juegosTablero.aplicacion.OntologiaJuegoBarcos;
import juegosTablero.aplicacion.barcos.ColocarBarcos;
import juegosTablero.aplicacion.barcos.EstadoJuego;
import juegosTablero.aplicacion.barcos.JuegoBarcos;
import juegosTablero.aplicacion.barcos.Localizacion;
import juegosTablero.aplicacion.barcos.MovimientoEntregado;
import juegosTablero.aplicacion.barcos.PosicionBarcos;
import juegosTablero.aplicacion.barcos.ResultadoMovimiento;
import juegosTablero.dominio.elementos.Juego;
import juegosTablero.dominio.elementos.JuegoAceptado;
import juegosTablero.dominio.elementos.Jugador;
import juegosTablero.dominio.elementos.Motivacion;
import juegosTablero.dominio.elementos.PedirMovimiento;
import juegosTablero.dominio.elementos.Posicion;
import juegosTablero.dominio.elementos.ProponerJuego;
import util.ArrayConversor;

/**
 *
 * @author manuelgallegochinchilla
 */
public class agenteJugadorBarquitos extends AgenteOntologiaJuegos{
   
    private int partidasActivas;
    private int partidasPreparacion;
    private static final int MAXPARTIDAS = 5;
    private HashMap<String, int[][]> partidas; //0 sin visitar - 1 agua - 2 tocado - 3 hundido
    private HashMap<String, Integer> ganarPartidas; //Voy quitando los barcos cuando se hunden para saber cuando gano
    private HashMap<String, Integer> perderPartidas; //Voy quitando los barcos cuando se hunden para saber cuando pierdo
    private int tamanoTableroX;
    private int tamanoTableroY;
    private int totalBarcos;
    private int portaaviones;
    private int acorazados;
    private int destructores;
    private int fragatas;
    private Jugador jugador;
    private ArrayConversor<Jugador> conversor;
   
    
    @Override
    protected void setup(){
        //Inicialización de variables
        super.setup();
        conversor = new ArrayConversor();
        jugador = new Jugador("Manu17", this.getAID());
        partidasActivas = 0;
        partidasPreparacion = 0;
        partidas = new HashMap<>();
        ganarPartidas = new HashMap<>();
        perderPartidas = new HashMap<>();
        tamanoTableroX = Vocabulario.FILAS_BARCOS;
        tamanoTableroY = Vocabulario.COLUMNAS_BARCOS;
        portaaviones = Vocabulario.NUM_PORTAAVIONES;
        acorazados = Vocabulario.NUM_ACORAZADOS;
        destructores = Vocabulario.NUM_DESTRUCTORES;
        fragatas = Vocabulario.NUM_FRAGATAS;
        guardarNumeroBarcos(); //Para guardar los barcos en sus variables
        
        //Añadir tareas principales
       MessageTemplate template = MessageTemplate.and(
				MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET),
				MessageTemplate.MatchPerformative(ACLMessage.CFP) );
        addBehaviour(new proponerJuegoResponder(this));
        addBehaviour(new jugarPartidaResponder(this, template));
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
        return NombreServicio.JUEGO_BARCOS.toString();
    }
    
   @Override 
    protected void takeDown(){
        //Desregristo del agente de las Páginas Amarillas
        super.takeDown();
        
        //Se liberan los recursos y se despide
        System.out.println("Finaliza la ejecución de " + this.getName());
    }
    
    
    
    //PROPONER JUEGO
    /**El AgenteCentralJuego tiene que localizar jugadores que estén dispuestos a
    Jugar un juego. El juego podrá ser una partida individual o un torneo.
    Para la comunicación entre los agentes se utilizará el protocolo Propose.**/
    
    private class proponerJuegoResponder extends ProposeResponder{
        
        public proponerJuegoResponder(Agent a){
            super(a, ProposeResponder.createMessageTemplate(FIPA_PROPOSE));
        }
        
        @Override
        protected ACLMessage prepareResponse(ACLMessage propuesta)  throws NotUnderstoodException {     
            try{
                ACLMessage msg = propuesta.createReply();
                ContentElement content = extraerMensaje(propuesta);
                if(content instanceof Action){
                    if(((Action) content).getAction() instanceof ProponerJuego){
                        ProponerJuego propuestajuego =  (ProponerJuego) ((Action) content).getAction();
                        if(partidasActivas < MAXPARTIDAS){                                                        
                            JuegoAceptado juegoAce = new JuegoAceptado(propuestajuego.getJuego(),jugador);
                            msg.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                            agenteJugadorBarquitos.this.mensajeFillContent(msg, juegoAce, OntologiaJuegoBarcos.getInstance());
                            partidasActivas++;
                        } else{
                            Motivacion motivacion = new Motivacion(propuestajuego.getJuego(),Motivo.JUEGOS_ACTIVOS_SUPERADOS);
                            msg.setPerformative(ACLMessage.REJECT_PROPOSAL);
                            agenteJugadorBarquitos.this.mensajeFillContent(msg, motivacion, OntologiaJuegoBarcos.getInstance());
                        }
                    }else{
                        if(((Action) content).getAction() instanceof ColocarBarcos){
                            ColocarBarcos colocarbarcos =  (ColocarBarcos) ((Action) content).getAction();
                            if(partidasPreparacion < MAXPARTIDAS){                
                                ArrayList<Localizacion> listaBarcos = ponerBarcosInicial();
                                jade.util.leap.ArrayList listaBar = conversor.fromJava2Jade( (ArrayList) listaBarcos);
                                PosicionBarcos posicionbarcos = new PosicionBarcos(colocarbarcos.getJuego(), listaBar);
                                msg.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                                agenteJugadorBarquitos.this.mensajeFillContent(msg, posicionbarcos, OntologiaJuegoBarcos.getInstance());
                                incluirPartida(colocarbarcos.getJuego().getIdJuego());
                                partidasPreparacion++;
                            } else{
                                Motivacion motivacion = new Motivacion(colocarbarcos.getJuego(),Motivo.JUEGOS_ACTIVOS_SUPERADOS);
                                msg.setPerformative(ACLMessage.REJECT_PROPOSAL);
                                agenteJugadorBarquitos.this.mensajeFillContent(msg, motivacion, OntologiaJuegoBarcos.getInstance());
                            }
                        }
                    }
                }else{
                    throw new NotUnderstoodException(propuesta.getOntology());
                }
                return msg;
                
            }catch (Exception ex){
                ex.printStackTrace();
                throw new NotUnderstoodException(propuesta.getOntology());
            } 
        }
    }
    
    
    //JUGAR PARTIDA
    /**Para jugar una partida se comunicará el AgenteTablero con los 
     * AgenteJugador que están implicados. Para ello utilizaremos el protocolo 
     * ContractNet para implementar la comunicación.*/
    
        /**
     * Clase respondedor del protocolo Contract-Net para realizar una oferta y
     * comprobar si ha sido aceoptada o no
     */
    private class jugarPartidaResponder extends ContractNetResponder {

        /**
         * Constructor parametrizado de la clase
         *
         * @param agente Agente que llama al protocolo
         * @param plantilla Mensaje CFP
         */
        public jugarPartidaResponder(Agent agente, MessageTemplate cfp) {          
            super(agente, cfp);
        }

        @Override
        protected ACLMessage handleCfp(ACLMessage cfp) throws NotUnderstoodException, RefuseException {
            ContentElement content = null;
            ACLMessage msg = cfp.createReply();
            try{              
                content = extraerMensaje(cfp);
            }catch (Exception ex){
                ex.printStackTrace();
                throw new NotUnderstoodException(cfp.getOntology());
            }
            if(content instanceof Action){
                if(((Action) content).getAction() instanceof PedirMovimiento){
                    PedirMovimiento pedirmov =  (PedirMovimiento) ((Action) content).getAction();
                    Random r = new Random();
                    if(pedirmov.getJugadorActivo().getAgenteJugador().equals(this.getAgent().getAID())){ //SI ME TOCA TURNO ENTRO AL IF                                   
                        Posicion pos = DevolverMov(pedirmov.getJuego().getIdJuego());
                        MovimientoEntregado mov = new MovimientoEntregado(pedirmov.getJuego(),pos);//Posicion del movimiento
                        msg.setPerformative(ACLMessage.PROPOSE);
                        try {
                            agenteJugadorBarquitos.this.mensajeFillContent(msg, mov, OntologiaJuegoBarcos.getInstance());
                        } catch (AgenteOntologiaJuegos.OntologiaDesconocida ex) {
                            Logger.getLogger(agenteJugadorBarquitos.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (Codec.CodecException ex) {
                            Logger.getLogger(agenteJugadorBarquitos.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (OntologyException ex) {
                            Logger.getLogger(agenteJugadorBarquitos.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }else{
                        msg.setPerformative(ACLMessage.PROPOSE); //SI NO MUEVO EN ESTE TURNO

                        try {
                            agenteJugadorBarquitos.this.mensajeFillContentNULO(msg, OntologiaJuegoBarcos.getInstance());
                        } catch (AgenteOntologiaJuegos.OntologiaDesconocida ex) {
                            Logger.getLogger(agenteJugadorBarquitos.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (Codec.CodecException ex) {
                            Logger.getLogger(agenteJugadorBarquitos.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (OntologyException ex) {
                            Logger.getLogger(agenteJugadorBarquitos.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }else{
                    throw new NotUnderstoodException(cfp.getOntology());
                }
            }else{
                throw new NotUnderstoodException(cfp.getOntology());
            }
            return msg;
        }

        /**
         * Metodo que espera el resultado del movimiento
         *
         * @param cfp Mensaje de oferta
         * @param propose Propuesta
         * @param accept Mensaje de aceptacion
         * @return inform-done o failure
         * @throws FailureException Excepcion
         */
        @Override
        protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose, ACLMessage accept){
            boolean miturno = false;
            ACLMessage mensaje = accept.createReply();
            //PRIMERO SACO SI EL RESULTADO ES DE MI TURNO
            try{
                ContentElement content = extraerMensaje(cfp);
                if(content instanceof Action){
                    if(((Action) content).getAction() instanceof PedirMovimiento){
                        PedirMovimiento pedirmov =  (PedirMovimiento) ((Action) content).getAction();
                            if(pedirmov.getJugadorActivo().getAgenteJugador().equals(this.getAgent().getAID())){ //SI ME TOCA TURNO ENTRO AL IF                     
                                miturno = true;
                            }         
                    }else{
                        throw new NotUnderstoodException(cfp.getOntology());
                    }
                }else{
                    throw new NotUnderstoodException(cfp.getOntology());
                }
            }catch (Exception ex){
                ex.printStackTrace();
                try {
                    throw new NotUnderstoodException(cfp.getOntology());
                } catch (NotUnderstoodException ex1) {
                    Logger.getLogger(agenteJugadorBarquitos.class.getName()).log(Level.SEVERE, null, ex1);
                }
            } 
                           
            try{  
                ContentElement content = extraerMensaje(accept);
                ResultadoMovimiento resultado = (ResultadoMovimiento)content;
                EstadoJuego estadoJ = new EstadoJuego(resultado.getJuego(),SEGUIR_JUGANDO);
                if(miturno && partidas.containsKey(resultado.getJuego().getIdJuego())){//SI ES MI TURNO Y NO HE ABANDONADO
                    int a = resultado.getMovimiento().getCoorX();
                    int b = resultado.getMovimiento().getCoorY();
                    if(resultado.getResultado().equals(Efecto.AGUA))
                        partidas.get(resultado.getJuego().getIdJuego())[a][b] = 1;

                    if(resultado.getResultado().equals(Efecto.TOCADO)){
                        partidas.get(resultado.getJuego().getIdJuego())[a][b] = 2; 
                        int n = ganarPartidas.get(resultado.getJuego().getIdJuego());
                        ganarPartidas.put(resultado.getJuego().getIdJuego(), n-1);
                        System.out.println(ganarPartidas.get(resultado.getJuego().getIdJuego())+ "PARA GANAR LAPARTIDA");
                        
                    }

                    if(resultado.getResultado().equals(Efecto.HUNDIDO)){
                        partidas.get(resultado.getJuego().getIdJuego())[a][b] = 3;
                        int n = ganarPartidas.get(resultado.getJuego().getIdJuego());
                        ganarPartidas.put(resultado.getJuego().getIdJuego(), n-1);
                        actualizarTablero(a,b,resultado.getJuego().getIdJuego());                       
                    }

                }else{
                    if(partidas.containsKey(resultado.getJuego().getIdJuego())){
                        if(resultado.getResultado().equals(Efecto.TOCADO)){
                            int n = perderPartidas.get(resultado.getJuego().getIdJuego());
                            perderPartidas.put(resultado.getJuego().getIdJuego(), n-1);
                        }  
                        if(resultado.getResultado().equals(Efecto.HUNDIDO)){
                            int n = perderPartidas.get(resultado.getJuego().getIdJuego());
                            perderPartidas.put(resultado.getJuego().getIdJuego(), n-1);
                        }      
                    }
                }
                
                if(!partidas.containsKey(resultado.getJuego().getIdJuego())){
                    estadoJ = new EstadoJuego(resultado.getJuego(),ABANDONO);                  
                }
                
                if(ganarPartidas.containsKey(resultado.getJuego().getIdJuego()) && ganarPartidas.get(resultado.getJuego().getIdJuego()) == 0){
                    partidasActivas--;
                    partidasPreparacion--;
                    System.out.println("PARTIDA GANADA");
                    partidas.remove(resultado.getJuego().getIdJuego());
                    ganarPartidas.remove(resultado.getJuego().getIdJuego());
                    perderPartidas.remove(resultado.getJuego().getIdJuego());
                    estadoJ = new EstadoJuego(resultado.getJuego(),GANADOR);
                    mensaje.setPerformative(ACLMessage.INFORM); //EN EL DIAGRAMA ES INFORM-DONE
                    agenteJugadorBarquitos.this.mensajeFillContent(mensaje, estadoJ, OntologiaJuegoBarcos.getInstance());
                    return mensaje;
                    
                }else{                  
                    if(perderPartidas.containsKey(resultado.getJuego().getIdJuego()) && perderPartidas.get(resultado.getJuego().getIdJuego()) == 0){
                        partidasActivas--;
                        partidasPreparacion--;
                        System.out.println("PARTIDA PERDIDA");                       
                        partidas.remove(resultado.getJuego().getIdJuego());
                        ganarPartidas.remove(resultado.getJuego().getIdJuego());
                        perderPartidas.remove(resultado.getJuego().getIdJuego());
                        estadoJ = new EstadoJuego(resultado.getJuego(),FIN_PARTIDA);
                    }                  
                }
                mensaje.setPerformative(ACLMessage.INFORM); //EN EL DIAGRAMA ES INFORM-DONE
                agenteJugadorBarquitos.this.mensajeFillContent(mensaje, estadoJ, OntologiaJuegoBarcos.getInstance());
            }catch (Exception ex){
                ex.printStackTrace();
                try {
                    throw new NotUnderstoodException(accept.getOntology());
                } catch (NotUnderstoodException ex1) {
                    Logger.getLogger(agenteJugadorBarquitos.class.getName()).log(Level.SEVERE, null, ex1);
                }
            } 
            return mensaje;
        }
        
        @Override
        protected void handleRejectProposal(ACLMessage cfp, ACLMessage propose, ACLMessage reject) {
				System.out.println("Agent "+getLocalName()+": Proposal rejected");
        }

        @Override
        protected void handleOutOfSequence(ACLMessage cfp, ACLMessage propose, ACLMessage msg) {
            super.handleOutOfSequence(cfp, propose, msg); //To change body of generated methods, choose Tools | Templates.
        }      
    }
    
    /**
     * Metodo utilizado para incluir la partida al almacenamiento del jugador
     * y tener una copia del tablero que se irá actualizando conforme se vaya jugando
     * @param idPartida 
     */
    void incluirPartida(String idPartida){
        int [][] tablero = new int[tamanoTableroX][tamanoTableroY];
        
        for(int i = 0; i < tamanoTableroX; i ++){
            for(int j = 0; j < tamanoTableroY; j ++){
                tablero[i][j] = 0;
            }
        }
        
        if(!partidas.containsKey(idPartida)){
            ganarPartidas.put(idPartida, totalBarcos);
            perderPartidas.put(idPartida, totalBarcos);
            partidas.put(idPartida, tablero); 
        }
    }
    
    
    /**
     * Función que nos procesa el estado de la partida y nos da el resultado 
     * más óptimo
     * @param juego
     * @return  Posición
     */
    Posicion DevolverMov(String juego){
        int tab[][] = partidas.get(juego);
        Random r = new Random();
        boolean aleatorio = true; // Si es el primer turno o no ha encontrado aun barco hace aleatorio
        int xTocado = 0;
        int yTocado = 0;
        Posicion pos = new Posicion();
        
        for (int i = 0; i < tamanoTableroX; i ++){
            for (int j = 0; j < tamanoTableroY; j ++){
                if(tab[i][j] == 2){
                    xTocado = i;
                    yTocado = j;
                    boolean salirBucle = false;
                    int corte = 0;
                    while (!salirBucle && corte < 4){
                        int a = r.nextInt(4);
                        switch(a){
                            case 0:
                                if(dentroTablero(xTocado - 1, yTocado) && tab[xTocado - 1][yTocado] == 0){
                                    pos.setCoorX(xTocado - 1);
                                    pos.setCoorY(yTocado);
                                    salirBucle = true;
                                }else{
                                    corte++;
                                }
                                break;

                            case 1:
                                if(dentroTablero(xTocado + 1, yTocado) && tab[xTocado + 1][yTocado] == 0){
                                    pos.setCoorX(xTocado + 1);
                                    pos.setCoorY(yTocado);
                                    salirBucle = true;
                                }else{
                                    corte++;
                                }
                                break;

                            case 2:
                                if(dentroTablero(xTocado, yTocado - 1) && tab[xTocado][yTocado - 1] == 0){
                                    pos.setCoorX(xTocado);
                                    pos.setCoorY(yTocado - 1);
                                    salirBucle = true;
                                }else{
                                    corte++;
                                }
                                break;

                            case 3:
                                if(dentroTablero(xTocado, yTocado + 1) && tab[xTocado][yTocado + 1] == 0){
                                    pos.setCoorX(xTocado);
                                    pos.setCoorY(yTocado + 1);
                                    salirBucle = true;
                                }else{
                                    corte++;
                                }
                                break;
                        }
                    }
                    if(salirBucle){
                        return pos;
                    }
                }
            }
        }
        
        if(aleatorio){ //Si no tenemos ningun tocado hacemos movimiento aleatorio
            boolean posValida = false;
            while(!posValida){             
                int cX = r.nextInt(tamanoTableroX);
                int cY = r.nextInt(tamanoTableroY);
                if(tab[cX][cY] == 0){ //Si esa posicion no ha sido visitada aún es válida para serlo
                    pos.setCoorX(cX);
                    pos.setCoorY(cY);
                    posValida = true;
                }
            }      
        }
        return pos;
    }
    
    /**
     * Función que nos comprueba si estamos dentro de los límites del tablero
     * o nos hemos salido
     * @param x
     * @param y
     * @return 
     */
    boolean dentroTablero(int x, int y){
        if(x >= 0 && x<tamanoTableroX && y >= 0 && y < tamanoTableroY)
            return true;
        else return false;
    }
    
    /**
     * Función que nos guarda el número total de casillas que debemos dejar en
     * TOCADO para ganar la partida
     */
    void guardarNumeroBarcos(){
        totalBarcos += ACORAZADO.getCasillas()*acorazados;
        totalBarcos += DESTRUCTOR.getCasillas()*destructores;
        totalBarcos += FRAGATA.getCasillas()*fragatas; 
        totalBarcos += PORTAAVIONES.getCasillas()*portaaviones;
    }
    
    /**
     * Función que nos pone los barcos en las posiciones iniciales cumpliendo con 
     * las restricciones del problema (entre barco y barco debe haber agua)
     * @return 
     */
    ArrayList<Localizacion> ponerBarcosInicial(){
      ArrayList<Localizacion> listaBarcos= new ArrayList<Localizacion>();
      int x = 0;
      int y = 0;
      Random r = new Random();
      boolean parada = false;
      
      int [][] tableroPrueba = new int[tamanoTableroX][tamanoTableroY];
      for(int i = 0; i < tamanoTableroX; i ++){
            for(int j = 0; j < tamanoTableroY; j ++){
                tableroPrueba[i][j] = 0;
            }
        }
      
        for (int i = 0; i < fragatas; i++){
            parada = false;
          do{
              x = r.nextInt(tamanoTableroX);
              y = r.nextInt(tamanoTableroY);
              if(validarDatos(tableroPrueba,x,y,FRAGATA, HORIZONTAL)){
                    Posicion p = new Posicion(x, y);
                    Localizacion l = new Localizacion(FRAGATA, p,HORIZONTAL);
                    listaBarcos.add(l);
                    parada = true;
              }
          }while(!parada); 
        }
        
        for (int i = 0; i < acorazados; i++){
            parada = false;
          do{
              x = r.nextInt(tamanoTableroX-1);
              y = r.nextInt(tamanoTableroY-1);
              if(validarDatos(tableroPrueba,x,y,ACORAZADO, VERTICAL)){
                    Posicion p = new Posicion(x, y);
                    Localizacion l = new Localizacion(ACORAZADO, p,VERTICAL);
                    listaBarcos.add(l);
                    parada = true;
              }
          }while(!parada); 
        }
            
        for (int i = 0; i < destructores; i++){
            parada = false;
          do{
              x = r.nextInt(tamanoTableroX);
              y = r.nextInt(tamanoTableroY);
              if(validarDatos(tableroPrueba,x,y,DESTRUCTOR, HORIZONTAL)){
                    Posicion p = new Posicion(x, y);
                    Localizacion l = new Localizacion(DESTRUCTOR, p,HORIZONTAL);
                    listaBarcos.add(l);
                    parada = true;
              }
          }while(!parada); 
        }

        for (int i = 0; i < portaaviones; i++){
            parada = false;
          do{
              x = r.nextInt(tamanoTableroX);
              y = r.nextInt(tamanoTableroY);
              if(validarDatos(tableroPrueba,x,y,PORTAAVIONES, VERTICAL)){
                    Posicion p = new Posicion(x, y);
                    Localizacion l = new Localizacion(PORTAAVIONES, p,VERTICAL);
                    listaBarcos.add(l);
                    parada = true;
              }
          }while(!parada); 
        }       
        return listaBarcos;          
    }
  
    
    /**
     * Funcion para comprobar los datos con respecto al barco y su orientación
     * @param tableroPrueba
     * @param x
     * @param y
     * @param tipobarco
     * @param orientacion
     * @return 
     */
    boolean validarDatos(int [][] tableroPrueba, int x, int y,TipoBarco tipobarco, Orientacion orientacion){
      boolean disponible = true;
      int a = x;
      int b = y;
      if(orientacion.equals(HORIZONTAL)){
          for(int i = 0; i < tipobarco.getCasillas(); i++){
              if(disponible && a < tamanoTableroX && b < tamanoTableroY){
                if(tableroPrueba[a][b] != 0){
                    disponible = false;
                }else{
                    a++;
                }             
              }else{
                  disponible = false;
              }
          }
          
      }else{
          for(int i = 0; i < tipobarco.getCasillas(); i++){
              if(disponible && a < tamanoTableroX && b < tamanoTableroY){
                if(tableroPrueba[a][b] != 0){
                  disponible = false;        
                }else{
                    b++;
                }
              }else{
                  disponible = false;
              }
          }
      }
      
      a = x;
      b = y;
      if(disponible){
          for(int i = 0; i < tipobarco.getCasillas(); i++){
              if(orientacion.equals(HORIZONTAL)){
                  tableroPrueba[a][b] = 1;
                  
                  if(b+1 < tamanoTableroY){
                    tableroPrueba[a][b+1] = 4;//Para que dos barcos no se junten
                    if(a-1 >= 0)
                        tableroPrueba[a-1][b+1] = 4;
                    if(a+1 < tamanoTableroX)
                        tableroPrueba[a+1][b+1] = 4;
                  }
                  
                  if(b-1 >= 0){
                    tableroPrueba[a][b-1] = 4;
                    if(a+1 < tamanoTableroX)
                        tableroPrueba[a+1][b-1] = 4;
                    if(a-1 >= 0)
                        tableroPrueba[a-1][b-1] = 4;
                  }
                  
                  if(a-1 >= 0){
                    if(tableroPrueba[a-1][b] != 1){
                      tableroPrueba[a-1][b] = 4;//Para que dos barcos no se junten
                      if(b-1 >= 0)
                        tableroPrueba[a-1][b-1] = 4;
                      if(b+1 < tamanoTableroY)
                        tableroPrueba[a-1][b+1] = 4;
                      
                    }
                  }
                  
                  if(a+1 < tamanoTableroX){
                    tableroPrueba[a+1][b] = 4;
                    if(b-1 >= 0)
                        tableroPrueba[a+1][b-1] = 4;
                    if(b+1 < tamanoTableroY)
                        tableroPrueba[a+1][b+1] = 4;
                  }
                  a++;
              }else{
                  tableroPrueba[a][b] = 1;
                  
                  if(a+1 < tamanoTableroX){
                    tableroPrueba[a+1][b] = 4;
                    if(b-1 >= 0)
                        tableroPrueba[a+1][b-1] = 4;
                    if(b+1 < tamanoTableroY)
                        tableroPrueba[a+1][b+1] = 4;
                  }
                  
                  if(a-1 >= 0){
                      tableroPrueba[a-1][b] = 4;//Para que dos barcos no se junten
                      if(b-1 >= 0)
                        tableroPrueba[a-1][b-1] = 4;
                      if(b+1 < tamanoTableroY)
                        tableroPrueba[a-1][b+1] = 4;
                  }
                  
                  if(b-1 >= 0){
                    if(tableroPrueba[a][b - 1] != 1){
                        tableroPrueba[a][b-1] = 4;//Para que dos barcos no se junten
                        if(a+1 < tamanoTableroX)
                            tableroPrueba[a+1][b-1] = 4;
                        if(a-1 >= 0)
                            tableroPrueba[a-1][b-1] = 4;
                    }
                  }
                  
                  if(b+1 < tamanoTableroY){
                    tableroPrueba[a][b+1] = 4;
                    if(a-1 >= 0)
                        tableroPrueba[a-1][b+1] = 4;
                    if(a+1 < tamanoTableroX)
                        tableroPrueba[a+1][b+1] = 4;
                  }
                  
                  b++;
              }
          }
      }
      return disponible;     
    }
  
    /**
     * Función que me actualiza el tablero cuando se realiza movimeinto
     * @param fila
     * @param columna
     * @param idjuego 
     */
    public void actualizarTablero(int fila,int columna,String idjuego){
        boolean parada = false;
        boolean parada1 = false;
        boolean p1 = false;
        boolean p2 = false;
        int n = 0;
        int a1 = fila;
        int a2 = fila;
        int b1 = columna;
        int b2 = columna;
        //0 sin visitar - 1 agua - 2 tocado - 3 hundido
      if(comprobarMovimiento(fila, columna, idjuego)){
        if(fila-1 >= 0)
            if(partidas.get(idjuego)[fila-1][columna] == 2)
                n = 1;
        if(fila+1 < tamanoTableroX)
            if(partidas.get(idjuego)[fila+1][columna] == 2)
                n = 1;
        if(columna-1 >= 0)
            if(partidas.get(idjuego)[fila][columna-1] == 2)
                n = 2;
        if(columna+1 < tamanoTableroY)
            if(partidas.get(idjuego)[fila][columna+1] == 2)
                n = 2;
        
        if(n == 0){

        }else{
            if(n == 1){                
                while(!parada && !parada1){
                    if(partidas.get(idjuego)[a1][columna] == 2){
                        partidas.get(idjuego)[a1][columna] = 3;
                        if(a1-1 >= 0){
                            a1--;
                        }else{
                            parada = true;
                        }
                    }else{                   
                        parada = true;
                    }
                                           
                    if(partidas.get(idjuego)[a2][columna] == 2){
                        partidas.get(idjuego)[a2][columna] = 3;
                        if(a2+1 < tamanoTableroX){
                            a2++;
                        }else{
                            parada1 = true;
                        }
                    }else{
                        parada1 = true;
                    }                
                }
            }
            
            if(n == 2){                
                while(!parada && !parada1){
                    if(partidas.get(idjuego)[fila][b1] == 2){
                        partidas.get(idjuego)[fila][b1] = 3;
                        if(b1-1 >= 0){
                            b1--;
                        }else{
                            parada = true;
                        }
                    }else{                   
                        parada = true;
                    }
                                           
                    if(partidas.get(idjuego)[fila][b2] == 2){
                        partidas.get(idjuego)[fila][b2] = 3;
                        if(b2+1 < tamanoTableroX){
                            b2++;
                        }else parada1 = true;
                    }else{
                        parada1 = true;
                    }                
                }
            }
        }
          
      }
    }
  
  /**
   * Función para comprobar el movimeinto
   * @param fila
   * @param columna
   * @param id
   * @return 
   */
  public boolean comprobarMovimiento(int fila,int columna, String id){
        boolean parada = false;
        boolean parada1 = false;
        boolean p1 = false;
        boolean p2 = false;
        int n = 0;
        int a1 = fila;
        int a2 = fila;
        int b1 = columna;
        int b2 = columna;
        //0 sin visitar - 1 agua - 2 tocado - 3 hundido
        if(fila-1 >= 0)
            if(partidas.get(id)[fila-1][columna] == 2)
                n = 1;
        if(fila+1 < tamanoTableroX)
            if(partidas.get(id)[fila+1][columna] == 2)
                n = 1;
        if(columna-1 >= 0)
            if(partidas.get(id)[fila][columna-1] == 2)
                n = 2;
        if(columna+1 < tamanoTableroY)
            if(partidas.get(id)[fila][columna+1] == 2)
                n = 2;
        
        if(n == 0){
            return false;
        }else{
            if(n == 1){                
                while(!parada && !parada1){
                    if(partidas.get(id)[a1][columna] == 2){
                        if(a1-1 >= 0){
                            a1--;
                        }else{
                            p1 = true;
                            parada = true;
                        }
                    }else{                   
                        if(partidas.get(id)[a1][columna] != 3){
                            p1 = true;
                        }
                        parada = true;
                    }
                                           
                    if(partidas.get(id)[a2][columna] == 2){
                        if(a2+1 < tamanoTableroX){
                            a2++;
                        }else p2 = true;
                    }else{
                        if(partidas.get(id)[a2][columna] != 3){
                            p2 = true;
                        }
                    }                
                }
            }
            
            if(n == 2){                
                while(!parada && !parada1){
                    if(partidas.get(id)[fila][b1] == 2){
                        if(b1-1 >= 0){
                            b1--;
                        }else{
                            p1 = true;
                            parada = true;
                        }
                    }else{                   
                        if(partidas.get(id)[fila][b1] != 3){
                            p1 = true;
                        }
                        parada = true;
                    }
                                           
                    if(partidas.get(id)[fila][b2] == 2){
                        if(b2+1 < tamanoTableroX){
                            b2++;
                        }else p2 = true;
                    }else{
                        if(partidas.get(id)[fila][b2] != 3){
                            p2 = true;
                        }
                    }                
                }
            }
        }
        return p1 && p2;
    }
}
