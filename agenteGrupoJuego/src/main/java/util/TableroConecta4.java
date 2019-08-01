/**
 * @author Julian - jso00008
 */
package util;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.Random;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import juegosTablero.Vocabulario;

    public class TableroConecta4 extends JPanel implements ComponentListener , ActionListener {
    
    //Inicializamos las variables
    private JButton[][] mCasillas = null ;
    
    private int mNumeroDeFilas = Vocabulario.FILAS_CONECTA_4 ;
    
    private int mNumeroDeColumnas = Vocabulario.COLUMNAS_CONECTA_4 ;
  
    private int mSeparacion = 0 ; 
    
    //Cargo los assets de las imágenes que usaré en la tablero
    private ImageIcon vacio2 = new ImageIcon("assets/VACIO.png");
    private ImageIcon vacio = new ImageIcon(vacio2.getImage().getScaledInstance(90, 90, java.awt.Image.SCALE_DEFAULT));
    private ImageIcon rojo2 = new ImageIcon("assets/ROJO.png");
    private ImageIcon rojo = new ImageIcon(rojo2.getImage().getScaledInstance(90, 90, java.awt.Image.SCALE_DEFAULT));
    private ImageIcon azul2 = new ImageIcon("assets/AZUL.png");
    private ImageIcon azul = new ImageIcon(azul2.getImage().getScaledInstance(90, 90, java.awt.Image.SCALE_DEFAULT));
    
 

    /**
     * El método se encarga de crear el tablero
     */
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
    
    /**
     * Constructor por defecto
     */
    public TableroConecta4() {        
        
        this.setBackground(Color.WHITE);
        
        this.addComponentListener(this);
        
        this.setSize( new Dimension(800, 600) );
          
        this.setLayout(null);              
        
    }
    
    /**
     * Método que se encarga de colocar la ficha en el tablero
     * @param fila Fila donde se va a colocar
     * @param col Columna donde se va a colocar
     * @param color Color que se va a pintar
     */
    public void colocarFicha(int fila, int col, Vocabulario.Color color){
        if(color.equals(Vocabulario.Color.ROJO)){
            mCasillas[fila][col].setIcon(rojo);
        }else if(color.equals(Vocabulario.Color.AZUL)){
            mCasillas[fila][col].setIcon(azul);
        }
    }
    
    /**
     * Pinta todo el tablero del color del ganador de la partida
     * @param color 
     */
    public void pintarGanador(Vocabulario.Color color){
        for(int i = 0; i < mNumeroDeFilas; i++){
            for(int j = 0; j < mNumeroDeColumnas; j++){
                if(color.equals(Vocabulario.Color.ROJO)){
                     mCasillas[i][j].setIcon(rojo);
                }else if(color.equals(Vocabulario.Color.AZUL)){
                   mCasillas[i][j].setIcon(azul);
                }
            }
        }
    }
    

    public void inicializar() {
        
        mCasillas = new JButton[mNumeroDeFilas][mNumeroDeColumnas];
        
        for( int fila = 0 ; fila < mNumeroDeFilas ; fila ++ ) {
            
            for( int columna = 0 ; columna < mNumeroDeColumnas ; columna ++ ) {
                
                JButton temp = new JButton();

                mCasillas[fila][columna] = temp;
                
                mCasillas[fila][columna].setIcon(vacio);
                
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

    private Random r = new Random();

    @Override
    public void actionPerformed(ActionEvent e) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    


}
