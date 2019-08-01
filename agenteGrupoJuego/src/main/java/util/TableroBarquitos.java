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
package util;

import java.awt.Color;
import static java.awt.Color.BLUE;
import static java.awt.Color.GREEN;
import static java.awt.Color.RED;
import static java.awt.Color.YELLOW;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import javax.swing.JButton;
import javax.swing.JPanel;
import juegosTablero.Vocabulario;
import static juegosTablero.Vocabulario.Color.AMARILLO;
import static juegosTablero.Vocabulario.Color.AZUL;
import static juegosTablero.Vocabulario.Color.ROJO;
import static juegosTablero.Vocabulario.Color.VERDE;

public class TableroBarquitos extends JPanel implements ComponentListener , ActionListener {

    /**
     * @return the cola
     */
    public Queue<colorear> getCola() {
        return cola;
    }
    
    private JButton[][] mCasillas = null ;
    private int numeroPintadasROJO = 0;
    private Queue<colorear> cola = new LinkedList<>(); 
    
    private int mNumeroDeFilas = Vocabulario.FILAS_BARCOS ; //CONSTANTE DEL VOCABULARIO
    
    private int mNumeroDeColumnas = Vocabulario.COLUMNAS_BARCOS ; //CONSTANTE DEL VOCABULARIO
    
    private int mSeparacion = 2 ; 
    
    public void acomodar() {
        
        int ancho = this.getWidth();
        
        int alto = this.getHeight();
        
        int dimensionMenor = Math.min( ancho , alto ); 
        
        int anchoDeCasilla = dimensionMenor / mNumeroDeColumnas ; 
        
        int altoDeCasilla = dimensionMenor / mNumeroDeFilas ;
        
        int xOffset = (ancho - dimensionMenor) / 2 ; 
        
        int yOffset = (alto - dimensionMenor) / 2 ; 
        
        for( int fila = 0 ; fila < mNumeroDeFilas ; fila ++ ) {
            
            for( int columna = 0 ; columna < mNumeroDeColumnas ; columna ++ ) {
                
                JButton temp = mCasillas[fila][columna] ;   
                
                temp.setBounds(xOffset + columna * anchoDeCasilla, yOffset + fila * altoDeCasilla, anchoDeCasilla - mSeparacion, altoDeCasilla - mSeparacion );
                            
            }            
        }                   
    }
    
    public void colorearFicha(){
        if(!cola.isEmpty() && numeroPintadasROJO < 21){
            colorear coloreo = getCola().remove();
            if(coloreo.getColor().equals(ROJO)){
                numeroPintadasROJO++;
                mCasillas[coloreo.getFila()][coloreo.getColumna()].setBackground(RED);
            }         

            if(coloreo.getColor().equals(AZUL))         
                mCasillas[coloreo.getFila()][coloreo.getColumna()].setBackground(BLUE);
            if(coloreo.getColor().equals(AMARILLO))         
                mCasillas[coloreo.getFila()][coloreo.getColumna()].setBackground(YELLOW);
            if(coloreo.getColor().equals(VERDE))         
                mCasillas[coloreo.getFila()][coloreo.getColumna()].setBackground(GREEN);

            //mCasillas[coloreo.getFila()][coloreo.getColumna()].setOpaque(true); //ESTE PARÁMETRO NECESARIO EN MAC
            //mCasillas[coloreo.getFila()][coloreo.getColumna()].setBorderPainted(false); //ESTE PARÁMETRO NECESARIO EN MAC
        }       
    }
    
    
    
     public void colocarFicha(int fila, int col, Vocabulario.Color color){
        colorear c = new colorear(col, fila, color);
        getCola().add(c);      
    }
    
    
    public TableroBarquitos() {        
        
        this.setBackground(Color.GRAY);
        
        this.addComponentListener(this);
          
        this.setLayout(null);              
        
    }

    public void inicializar() {
        
        mCasillas = new JButton[mNumeroDeFilas][mNumeroDeColumnas];
        
        for( int fila = 0 ; fila < mNumeroDeFilas ; fila ++ ) {
            
            for( int columna = 0 ; columna < mNumeroDeColumnas ; columna ++ ) {
                
                JButton temp = new JButton();             
                temp.addActionListener(this);
                
                mCasillas[fila][columna] = temp;  
                this.add(temp);          
            }                           
        }
    }

    public void componentResized(ComponentEvent e) {
        
        this.acomodar();
        
    }

    public void componentMoved(ComponentEvent e) {
    }

    public void componentShown(ComponentEvent e) {
    }

    public void componentHidden(ComponentEvent e) {
    }

    public void setNumeroDeFilas(int mNumeroDeFilas) {
        this.mNumeroDeFilas = mNumeroDeFilas;
    }

    public int getNumeroDeFilas() {
        return mNumeroDeFilas;
    }

    public void setNumeroDeColumnas(int mNumeroDeColumnas) {
        this.mNumeroDeColumnas = mNumeroDeColumnas;
    }

    public int getNumeroDeColumnas() {
        return mNumeroDeColumnas;
    }

    public void actionPerformed(ActionEvent e) {               
    }

}
