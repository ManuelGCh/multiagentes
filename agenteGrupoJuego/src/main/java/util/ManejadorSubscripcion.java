/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util;

import jade.core.AID;
import jade.core.Agent;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.SubscriptionResponder.Subscription;
import jade.proto.SubscriptionResponder;
import java.util.List;

/**
 *
 * @author javiermq
 */
public class ManejadorSubscripcion extends SubscriptionResponder {
       
        GestorSuscripciones manager;
        AID agente;
        public ManejadorSubscripcion(Agent a, MessageTemplate mt, GestorSuscripciones sm) {
            super(a, mt);
            this.agente = a.getAID();
            this.manager=sm;
        }
         @Override
        protected ACLMessage handleSubscription(ACLMessage subscription) throws NotUnderstoodException, RefuseException {
            
            
            String nombreAgente = subscription.getSender().getName();
            System.out.println("Recibida la solicitud de subscripción de "+nombreAgente);
           
            try{
                SubscriptionResponder.Subscription sub= this.createSubscription(subscription);
                
                if(manager.containsKey(nombreAgente) ){
                    ACLMessage agree = subscription.createReply();
                    agree.setPerformative(ACLMessage.REFUSE);
                    return agree;
                }
                
                    manager.register(sub);

                    ACLMessage agree = subscription.createReply();
                    agree.setPerformative(ACLMessage.AGREE);
                    return agree;
                

            }catch(Exception e){
                    e.printStackTrace();
                    ACLMessage agree = subscription.createReply();
                    agree.setPerformative(ACLMessage.REFUSE);
                    return agree;
            }
            
        }
        
         @Override
        protected ACLMessage handleCancel(ACLMessage cancel) throws FailureException {
            try{
                String nombreAgente = cancel.getSender().getName();
                System.out.print("Recibida la solicitud de cancelacion de subscripción de "+nombreAgente);
                SubscriptionResponder.Subscription sub = manager.getSuscripcion(nombreAgente);
                manager.deregister(sub);
                ACLMessage agree = cancel.createReply();
                agree.setPerformative(ACLMessage.INFORM);
                return agree;
            }catch(Exception e){
                    e.printStackTrace();
                    ACLMessage agree = cancel.createReply();
                    agree.setPerformative(ACLMessage.REFUSE);
                    return agree;
            }
        }
}
