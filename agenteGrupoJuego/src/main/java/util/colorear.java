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

import juegosTablero.Vocabulario.Color;

/**
 *
 * @author manuelgallegochinchilla
 */
public class colorear {
    private int columna;
    private int fila;
    private Color color;
    
    public colorear(int c, int f, Color colo){
        this.columna = c;
        this.fila = f;
        this.color = colo;
    }

    /**
     * @return the columna
     */
    public int getColumna() {
        return columna;
    }

    /**
     * @return the fila
     */
    public int getFila() {
        return fila;
    }

    /**
     * @return the color
     */
    public Color getColor() {
        return color;
    }
    
}
