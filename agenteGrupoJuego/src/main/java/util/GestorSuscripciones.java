/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util;

import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.proto.SubscriptionResponder;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author javiermq
 */
public class GestorSuscripciones extends HashMap<String, SubscriptionResponder.Subscription> implements SubscriptionResponder.SubscriptionManager {
  

    public GestorSuscripciones() {
       super();
    }

    @Override
    public boolean register(SubscriptionResponder.Subscription s) throws RefuseException, NotUnderstoodException {
        // Guardamos la suscripción asociada al agente que la solita
        String nombreAgente = s.getMessage().getSender().getName();
        this.put(nombreAgente, s);
        return true;
    }

    @Override
    public boolean deregister(SubscriptionResponder.Subscription s) throws FailureException {
        // Eliminamos la suscripción asociada a un agente
        String nombreAgente = s.getMessage().getSender().getName();
        this.remove(nombreAgente);
        s.close(); // queda cerrada la suscripción
        return true;
    }
    
    public SubscriptionResponder.Subscription getSuscripcion( String nombreAgente ) {
        return this.get(nombreAgente);
    }
    
   
}
