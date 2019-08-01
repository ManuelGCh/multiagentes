/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util;


import jade.core.AID;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.SubscriptionInitiator;

/**
 *
 * @author javiermq
 */
class SubscripcionIniciador extends SubscriptionInitiator {

    boolean finalizar;

    ///>Almacená en orden de llegada las ofertas para luego pagar en función de si tiene dinero

    public SubscripcionIniciador(Agent a, ACLMessage msg) {
        super(a, msg);

        }
    
  
    @Override
    protected void handleAgree(ACLMessage agree) {
        System.out.println("Solicitud aceptada");
    }

    @Override
    protected void handleRefuse(ACLMessage agree) {
        System.out.println("Solicitud rechazada");
    }

    @Override
    protected void handleInform(ACLMessage inform) {
        System.out.println("Solicitud inform");
    }
    
    

    @Override
    public void cancellationCompleted(AID agent) {
        MessageTemplate template = MessageTemplate.MatchSender(agent);
        ACLMessage msg = myAgent.blockingReceive(template);
        if (msg.getPerformative() == ACLMessage.INFORM) {
            System.out.println("Cancelación de subscripcion correcta ");
        } else {
            System.out.println("Cancelación de subscripcion INcorrecta ");
        }
    }

    @Override
    protected void handleFailure(ACLMessage msg) {
        if (msg.getSender().equals(myAgent.getAMS())) {
            System.out.println("El agente no ha podido ser encontrado en las paginas amarillas");
        } else {
            System.out.println("Error al enviar el mensaje " + msg);
        }

    }
}
