/**
 * @author Julian - jso00008
 */
package GUI;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JFrame;
import util.TableroConecta4;

    public class MainFrameConecta4 extends JFrame {
    
    private TableroConecta4 mTablero = new TableroConecta4();
    
        public MainFrameConecta4(String titulo) {
            try {
                jbInit(titulo);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        public TableroConecta4 getTablero(){
            return mTablero;
        }

        private void jbInit(String titulo) throws Exception {
            this.setTitle(titulo);
            this.setLayout(new BorderLayout());
            this.setSize( new Dimension(800, 600) );
            this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            this.add(mTablero, BorderLayout.CENTER );

            mTablero.inicializar();

            }
        }
