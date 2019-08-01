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
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import static jade.domain.FIPANames.InteractionProtocol.FIPA_PROPOSE;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREResponder;
import jade.proto.ProposeResponder;
import jade.proto.SubscriptionResponder;
import jade.wrapper.StaleProxyException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import juegosTablero.Vocabulario;
import juegosTablero.Vocabulario.Motivo;
import juegosTablero.Vocabulario.NombreServicio;
import juegosTablero.aplicacion.OntologiaJuegoBarcos;
import juegosTablero.aplicacion.OntologiaJuegoConecta4;
import juegosTablero.dominio.elementos.ClasificacionJuego;
import juegosTablero.dominio.elementos.CompletarJuego;
import juegosTablero.dominio.elementos.Grupo;
import juegosTablero.dominio.elementos.Juego;
import juegosTablero.dominio.elementos.JuegoAceptado;
import juegosTablero.dominio.elementos.Jugador;
import juegosTablero.dominio.elementos.Motivacion;
import util.GestorSuscripciones;
import util.ManejadorSubscripcion;

/**
 *
 * @author manuelgallegochinchilla
 */
public class agenteGrupoJuegos extends AgenteOntologiaJuegos{
    GestorSuscripciones gestor;  
    private HashMap<String,ClasificacionJuego> clasificaciones;
    private HashMap<String,CompletarJuego> tableros;
    private int partidasActivas;
    private final int MAXPARTIDAS = 5;
    private Grupo agente;
    private ArrayList<String> partidasJugadas;
    
    @Override
    protected void setup(){
        //Inicialización de variables
        super.setup();
        gestor =new GestorSuscripciones();
        partidasActivas = 0;
        partidasJugadas = new ArrayList<>();
        clasificaciones = new HashMap<>();
        tableros = new HashMap<>();
        gestor = new GestorSuscripciones();
        MessageTemplate plantilla = MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_SUBSCRIBE);
        agente = new Grupo("AgentsDevelopers", this.getAID());
        
        //Añadir tareas principales
        addBehaviour(new completarJuegoResponder(this));
        addBehaviour(new TareaRecibirMensaje(this, 5000));
        addBehaviour(new TareaInformarJuego(this, plantilla, gestor)); 
    }
    
    @Override
    public Ontology [] defineOntology() throws Exception{
        Ontology[] ret = {
            OntologiaJuegoBarcos.getInstance(),
            OntologiaJuegoConecta4.getInstance()
        };
        return ret;
    }
    
    @Override
    public String nombreRegistro(){
        return NombreServicio.GRUPO_JUEGOS.toString();
    }
    
    @Override 
    protected void takeDown(){
        //Desregristo del agente de las Páginas Amarillas
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
        }
        
        //Se liberan los recursos y se despide
        System.out.println("Finaliza la ejecución de " + this.getName());
    }
    
    
    /**
     * Tarea que recibe los mensajes de los tableros una vez han acabado las partidas
     * Recibe clasificaciones finales
     */
    public class TareaRecibirMensaje extends TickerBehaviour {

        public TareaRecibirMensaje(Agent a, long period) {
            super(a, period);
        }


        @Override
        protected void onTick() {
            MessageTemplate plantilla = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            ACLMessage mensaje = myAgent.receive(plantilla);
            if (mensaje != null) {
                ContentElement msg = null;
                try {
                    msg = extraerMensaje(mensaje);
                } catch (Exception ex) {
                    Logger.getLogger(agenteGrupoJuegos.class.getName()).log(Level.SEVERE, null, ex);
                }
                ClasificacionJuego cl = (ClasificacionJuego) msg;
                clasificaciones.put(cl.getJuego().getIdJuego(), cl);
                addBehaviour(new InformarJuegoASubs(cl.getJuego().getIdJuego()));               
            } 
            else
                block();          
        }
    }
   
    
    //COMPLETAR JUEGO
    /**El AgenteCentralJuego, cuando ya tenga los jugadores para un juego, 
    solicita a un AgenteGrupoJuegos que complete el juego propuesto con la 
    lista de jugadores que lo forman. la comunicación entre los agentes se 
    utilizará el protocolo Propose.**/
    private class completarJuegoResponder extends ProposeResponder{
        
        public completarJuegoResponder(Agent a){
            super(a, ProposeResponder.createMessageTemplate(FIPA_PROPOSE));
        }
        
         @Override
        protected ACLMessage prepareResponse(ACLMessage propuesta)  throws NotUnderstoodException {
            
            String idPartida = "";
            ACLMessage msg = propuesta.createReply();
            try{
                ContentElement content = extraerMensaje(propuesta);
                if(content instanceof Action){
                    if(((Action) content).getAction() instanceof CompletarJuego){
                        CompletarJuego propuestajuego =  (CompletarJuego) ((Action) content).getAction();
                        if(partidasActivas < MAXPARTIDAS){                           
                            if(propuestajuego.getJuego().getTipoJuego().equals(Vocabulario.TipoJuego.BARCOS)){
                                JuegoAceptado juegoAce = new JuegoAceptado(propuestajuego.getJuego(),agente);
                                msg.setPerformative(ACLMessage.ACCEPT_PROPOSAL);                              
                                agenteGrupoJuegos.this.mensajeFillContent(msg, juegoAce, OntologiaJuegoBarcos.getInstance());
                                partidasActivas++;
                                addBehaviour(new TareaCrearTablero("BARCOS", propuestajuego));
                            }else{
                                if(propuestajuego.getJuego().getTipoJuego().equals(Vocabulario.TipoJuego.CONECTA_4)){
                                    msg.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                                    JuegoAceptado juego = new JuegoAceptado(propuestajuego.getJuego(), agente);
                                    agenteGrupoJuegos.this.mensajeFillContent(msg, juego, OntologiaJuegoConecta4.getInstance());
                                    partidasActivas++;
                                    addBehaviour(new TareaCrearTablero("CONECTA4", propuestajuego));
                                }else{
                                    msg.setPerformative(ACLMessage.REJECT_PROPOSAL);                                    
                                    Motivacion motivo = new Motivacion(propuestajuego.getJuego(), Motivo.TIPO_JUEGO_NO_IMPLEMENTADO);
                                    agenteGrupoJuegos.this.mensajeFillContent(msg, motivo, OntologiaJuegoConecta4.getInstance());
                                }
                                
                            }
                        } else{
                            Motivacion motivacion = new Motivacion(propuestajuego.getJuego(),Motivo.JUEGOS_ACTIVOS_SUPERADOS);
                            msg.setPerformative(ACLMessage.REJECT_PROPOSAL);
                            agenteGrupoJuegos.this.mensajeFillContent(msg, motivacion, OntologiaJuegoBarcos.getInstance());
                        }
                    }else{
                        throw new NotUnderstoodException(propuesta.getOntology());
                    }
                }else{
                    throw new NotUnderstoodException(propuesta.getOntology());
                }
            }catch (Exception ex){
                ex.printStackTrace();
                throw new NotUnderstoodException(propuesta.getOntology());
            }
            return msg;
        }
    }
    
    /**
     * Tarea que crea los tableros según sea barquitos o conecta4
     */
    public class TareaCrearTablero extends OneShotBehaviour{
        
        CompletarJuego propuestajuego;
        String tipojuego;
        public TareaCrearTablero(String a, CompletarJuego c){
            propuestajuego = c;
            tipojuego = a;
        }
        
        @Override
        public void action() {
            Object[] objetos = new Object[6];
            objetos[0] = propuestajuego.getListaJugadores();
            objetos[1] = propuestajuego.getJuego().getMinVictorias();
            objetos[2] = propuestajuego.getJuego().getModoJuego();
            objetos[3] = propuestajuego.getJuego().getIdJuego();                            
            objetos[4] = myAgent.getAID();
            if(partidasJugadas.contains(propuestajuego.getJuego().getIdJuego())){ //SI YA LA HEMOS JUGADO REPRODUCIMOS
                objetos[5] = true;
           }else{
                objetos[5] = false;
                partidasJugadas.add(propuestajuego.getJuego().getIdJuego());
            }
     
            String idPartida = propuestajuego.getJuego().getIdJuego();            
            tableros.put(idPartida, propuestajuego);
            clasificaciones.put(idPartida, new ClasificacionJuego());
            
            if(tipojuego.equals("BARCOS")){
                String nombreTablero = "TableroBarquitos_"+idPartida;
                try {
                    myAgent.getContainerController().createNewAgent(nombreTablero,
                        "agentes.agenteTableroBarquitos",objetos).start();
                } catch (StaleProxyException ex) {
                    Logger.getLogger(agenteGrupoJuegos.class.getName()).log(Level.SEVERE, null, ex);
                }
            }else{
                String nombreTablero = "TableroConecta4_"+idPartida;
                try {
                    myAgent.getContainerController().createNewAgent(nombreTablero,
                        "agentes.agenteTableroConecta4", objetos).start();
                } catch (StaleProxyException ex) {
                    Logger.getLogger(agenteGrupoJuegos.class.getName()).log(Level.SEVERE, null, ex);
                }
            }                                        
        }        
    }
    
    
    //INFORMAR JUEGO
    /**AgenteCentalJuego, una vez que confirme un juego con un AgenteGrupoJuegos,
    realiza una suscripción para que se le informe de la clasificación final 
    para los jugadores que han participado en el juego. Para ello se utilizará 
    el protocolo Subscribe.**/
    public class InformarJuegoASubs extends OneShotBehaviour{
        
        String idPart;
        public InformarJuegoASubs(String a){
            idPart = a;
        }
        
        @Override
        public void action() {
            //Se envía el resultado final del juego al agente central
            ACLMessage mensaje = new ACLMessage(ACLMessage.INFORM);
            mensaje.setSender(myAgent.getAID());
            try {
                agenteGrupoJuegos.this.mensajeFillContent(mensaje, clasificaciones.get(idPart), OntologiaJuegoBarcos.getInstance());
            } catch (BeanOntologyException ex) {
                Logger.getLogger(agenteGrupoJuegos.class.getName()).log(Level.SEVERE, null, ex);
            } catch (AgenteOntologiaJuegos.OntologiaDesconocida ex) {
                Logger.getLogger(agenteGrupoJuegos.class.getName()).log(Level.SEVERE, null, ex);
            } catch (Codec.CodecException ex) {
                Logger.getLogger(agenteGrupoJuegos.class.getName()).log(Level.SEVERE, null, ex);
            } catch (OntologyException ex) {
                Logger.getLogger(agenteGrupoJuegos.class.getName()).log(Level.SEVERE, null, ex);
            }
            //Se añade como destinatario a todos los agentes suscritos
            for(SubscriptionResponder.Subscription subscripcion:gestor.values()){
                subscripcion.notify(mensaje);
            }      
        }        
    }
    
     /**
     * Tarea que gestiona la suscripción para informar al agente cental de juegos cuando
     * un juego ha terminado con la clasificación de ese juego
     */
    class TareaInformarJuego extends SubscriptionResponder {
        private SubscriptionResponder.Subscription suscripcionJugador;
        
        public TareaInformarJuego(Agent a, MessageTemplate mt) {
            super(a, mt);
        }

        public TareaInformarJuego(Agent a, MessageTemplate mt, SubscriptionResponder.SubscriptionManager sm) {
            super(a, mt, sm);
        }
        
        @Override
        protected ACLMessage handleSubscription(ACLMessage subscription) throws NotUnderstoodException, RefuseException {
            
            String nombreAgente = subscription.getSender().getName();
            suscripcionJugador = createSubscription(subscription);
            if (!gestor.containsKey(nombreAgente)) {
                mySubscriptionManager.register(suscripcionJugador);            
            } else {
                // Ya tenemos una suscripción anterior del jugador y no 
                // volvemos a registrarlo.
            }
            ACLMessage agree = subscription.createReply();
            agree.setPerformative(ACLMessage.AGREE);
            return agree;
        }
        
        @Override
        protected ACLMessage handleCancel(ACLMessage cancel) throws FailureException {
            String nombreAgente = cancel.getSender().getName();
            suscripcionJugador = gestor.getSuscripcion(nombreAgente);
            mySubscriptionManager.deregister(suscripcionJugador);                      
            return null; // no hay que enviar mensaje de confirmación
        }
    }   
}
