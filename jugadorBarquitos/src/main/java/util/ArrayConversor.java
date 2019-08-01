/*
 * Copyright (C) 2019 Julian
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
package util;

import jade.content.Concept;
import java.util.Iterator;

/**
 *
 * @author Julian
 */
public class ArrayConversor <T>{
    public jade.util.leap.ArrayList fromJava2Jade(java.util.ArrayList<Concept> list){
        jade.util.leap.ArrayList ret=new jade.util.leap.ArrayList();
        for(Concept c:list)
            ret.add(c);
        return ret;
    }
   
    public  java.util.ArrayList<T> fromJade2Java(jade.util.leap.ArrayList list, Class<T> type){
         java.util.ArrayList<T> ret=new java.util.ArrayList<T>();
         for(Iterator it=list.iterator();it.hasNext();){
             T o2=(T)it.next();
             ret.add(o2);
         }
         return ret;
     }
}
