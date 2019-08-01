/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package agentes;

import jade.content.Concept;
import jade.content.ContentElement;
import juegosTablero.*;
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
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.proto.ProposeInitiator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import juegosTablero.Vocabulario.ModoJuego;
import juegosTablero.Vocabulario.NombreServicio;
import juegosTablero.Vocabulario.TipoJuego;
import static juegosTablero.Vocabulario.TIPO_SERVICIO;
import juegosTablero.aplicacion.OntologiaJuegoBarcos;
import juegosTablero.aplicacion.OntologiaJuegoConecta4;
import juegosTablero.aplicacion.OntologiaJuegoDomino;
import juegosTablero.aplicacion.barcos.JuegoBarcos;
import juegosTablero.aplicacion.conecta4.JuegoConecta4;
import juegosTablero.aplicacion.domino.JuegoDomino;
import juegosTablero.dominio.elementos.Juego;
import juegosTablero.dominio.elementos.ProponerJuego;

/**
 * Clase de comportamientos generales de un agentes en contexto de juegos
 * @author  javiermq
 */
public class AgenteOntologiaJuegos extends Agent {

    private final Codec codec = new SLCodec();

    //mapa de ontologuas
    private HashMap<Ontology,ContentManager> registroOntologias=new HashMap<Ontology,ContentManager>();
    private String tipoServicio;//reee
    
    /**
     * Registra una ontologia en el mapa
     * @param ontologia 
     */
    private void registraOntologia(Ontology ontologia){
        ContentManager manager = (ContentManager) getContentManager();
        manager.registerLanguage(codec);
	manager.registerOntology(ontologia);
        this.registroOntologias.put(ontologia, manager);
    }
    
    /**
     * Encuentra el manager de una determinada ontologia
     * @param nOntologia
     * @return
     * @throws agentes.AgenteOntologiaJuegos.OntologiaDesconocida 
     */
    protected ContentManager findManager(String nOntologia) throws OntologiaDesconocida{
        for(Ontology ontologia:registroOntologias.keySet())
            if(ontologia.getName().equals(nOntologia))
                return registroOntologias.get(ontologia);
        
        throw new OntologiaDesconocida(nOntologia);
    } 
    
    /**
     * Define las ontologias que se van a registrar
     * @return
     * @throws Exception 
     */
    public  Ontology [] defineOntology() throws Exception{
           Ontology[] ret = { 
                OntologiaJuegoBarcos.getInstance(),
                OntologiaJuegoConecta4.getInstance(),
                OntologiaJuegoDomino.getInstance()
        };
           return ret;
    } 
    
    /**
     * Al iniciar hace dos cosas: 1) registra sus ontologias, 2) registro en paginas amarillas
     */
    @Override
    protected void setup() {
        System.out.println("Inicia la ejecución de " + this.getName());
        
        try {
            for(Ontology ontology:defineOntology())
                registraOntologia(ontology);
            registroPaginasAmarillas();
        } catch (Exception ex) {
            Logger.getLogger(AgenteOntologiaJuegos.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    /**
     * metodo de registro de páginas amarillas, si es null, no se registra
     * @return 
     */
    public String nombreRegistro(){
        return null;
    }    

    /**
     * Al salir nos deregistramos
     */
    @Override
    protected void takeDown() {
        //Desregistro de las Páginas Amarillas si no es null
        if(nombreRegistro()==null)
            try {
                DFService.deregister(this);
            }
                catch (FIPAException fe) {
                fe.printStackTrace();
            }
        
        System.out.println("Finaliza la ejecución de " + this.getName());
    }
    /**
     * Funcion de registro en paginas amarillas 
     */
    private void registroPaginasAmarillas() throws Exception{
        if(nombreRegistro()==null) 
            return;
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
	ServiceDescription sd = new ServiceDescription();
	sd.setType(TIPO_SERVICIO);
	sd.setName(nombreRegistro());
	dfd.addServices(sd);

        DFService.register(this, dfd);
	
    }
    
        public ACLMessage mensajeFillContent(ACLMessage msg, ContentElement content, Ontology ontologia) throws OntologiaDesconocida, Codec.CodecException, OntologyException{
        if(!this.registroOntologias.containsKey(ontologia))
            throw new OntologiaDesconocida(ontologia);
        
        msg.setOntology(ontologia.getName());
        msg.setLanguage(codec.getName());
        ContentManager manager = registroOntologias.get(ontologia);
        manager.fillContent(msg, content);
        return msg;
    }
        
    public ACLMessage mensajeFillContentNULO(ACLMessage msg, Ontology ontologia) throws OntologiaDesconocida, Codec.CodecException, OntologyException{
        if(!this.registroOntologias.containsKey(ontologia))
            throw new OntologiaDesconocida(ontologia);
        
        msg.setOntology(ontologia.getName());
        msg.setLanguage(codec.getName());
        return msg;
    }
    
    
    /**
     * Funcion de extracción de contenido de un mensaje. En funcion de la ontologia, recupera su manager y extrae el contenido.
     * @param propuesta
     * @return
     * @throws Exception 
     */
    protected ContentElement extraerMensaje(ACLMessage propuesta) throws Exception{
           try {
                System.out.println("message :"+propuesta.getContent());
                ContentManager manager=findManager(propuesta.getOntology());
                System.out.println("manager :"+manager);
                ContentElement content = manager.extractContent(propuesta);
                return content;
                
            } catch (Exception ex) {
                //ex.printStackTrace();
                System.out.println("No entiendo en el mensaje 1");
                throw new NotUnderstoodException(propuesta.getOntology());
            }
    }

    /**
     * construye un mensaje de una ontologia con la configuracion basica del 
     * @param receivers
     * @param content
     * @param ontologia
     * @return
     * @throws Exception 
     */
    public ACLMessage mensajeProposal(List<AID> receivers, ContentElement content, Ontology ontologia) throws Exception{
        
        if(!this.registroOntologias.containsKey(ontologia)) 
            throw new OntologiaDesconocida(ontologia);
        ACLMessage msg = new ACLMessage(ACLMessage.PROPOSE);
        msg.setProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE);
        msg.setSender(this.getAID());
        msg.setLanguage(codec.getName());
        msg.setOntology(ontologia.getName());
        
        for(AID receiver:receivers)
         msg.addReceiver(receiver);

       registroOntologias.get(ontologia).fillContent(msg, content);
      
        return msg;
    }
    
    

    /**
     * Funcion de apoyo para obtener el Juego por tipo de juego 
     * @param tipoJuego
     * @return
     * @throws agentes.AgenteOntologiaJuegos.OntologiaDesconocida 
     */    
    public Concept getSelectedConceptJuego(TipoJuego tipoJuego)throws OntologiaDesconocida{
        if(tipoJuego.equals(TipoJuego.BARCOS))
            return new JuegoBarcos();
        if(tipoJuego.equals(TipoJuego.CONECTA_4))
            return new JuegoConecta4();
        if(tipoJuego.equals(TipoJuego.DOMINO))
            return new JuegoDomino();
       throw new OntologiaDesconocida(tipoJuego);
    }
    
    /**
     * Funcion de apoyo para obtener ontologia por tipo de juego 
     * @param tipoJuego
     * @return
     * @throws Exception 
     */
    public Ontology getSelectedOntologia(TipoJuego tipoJuego)throws Exception{
        if(tipoJuego.equals(TipoJuego.BARCOS))
            return OntologiaJuegoBarcos.getInstance();
        if(tipoJuego.equals(TipoJuego.CONECTA_4))
            return OntologiaJuegoConecta4.getInstance();
        if(tipoJuego.equals(TipoJuego.DOMINO))
            return OntologiaJuegoDomino.getInstance();
       throw new OntologiaDesconocida(tipoJuego);
    }
    /**
     * Exception si no llegan ontologias que no conocemos
     */
    static public class OntologiaDesconocida extends Exception{
        OntologiaDesconocida(String name){
            super("No se conoce "+name);
        }
        OntologiaDesconocida(Ontology ontologia){
            super("No se conoce "+ontologia.getName());
        }
        OntologiaDesconocida(TipoJuego tipoJuego){
            super("No se conoce "+tipoJuego);
        }
        OntologiaDesconocida(NombreServicio tipoJuego){
            super("No se conoce "+tipoJuego);
        }
    }
    
    /**
     * Comportamiento generico para proponer a otros agentes y recuperar los que han aceptado
     */
    static class ComportamientoProponerAgentes extends ProposeInitiator {

       List<AID> agentesPropuesta;

       List<AID> agentesAceptan;
        public ComportamientoProponerAgentes(Agent myAgent, ACLMessage msg, List<AID> agentesPropuesta) {
            super(myAgent,msg);
            this.agentesPropuesta=agentesPropuesta;
            this.agentesAceptan=new LinkedList<>();
        }

        @Override
        protected void handleAcceptProposal(ACLMessage msg) {
            System.out.println(msg.getSender().getLocalName() + " acepta ");   
            this.agentesAceptan.add(msg.getSender());
        }
        

        @Override
        protected void handleRejectProposal(ACLMessage msg) {
              System.out.println(msg.getSender().getLocalName() + " rechaza.");      
        }


        @Override
        protected void handleAllResponses(Vector responses) {
           System.out.println("Al final, aceptan " + this.agentesAceptan);
        }
    }    
}
