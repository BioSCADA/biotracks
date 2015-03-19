package com.google.android.apps.mytracks.util;

/*
 * @copyright Copyright (c) 2010 Laboratório de Educação Cerebral. (http://www.educacaocerebral.com.br)
 *
 * This file is part of SoftVFC.
 *
 * SoftVFC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SoftVFC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SoftVFC.  If not, see <http://www.gnu.org/licenses/>.
 *
 * *********************
 *
 * Este arquivo é parte do programa SoftVFC.
 *
 * SoftVFC é um software livre; você pode redistribui-lo e/ou
 * modifica-lo dentro dos termos da Licença Pública Geral GNU como
 * publicada pela Fundação do Software Livre (FSF); na versão 3 da
 * Licença.
 *
 * Este programa é distribuido na esperança que possa ser util,
 * mas SEM NENHUMA GARANTIA; sem uma garantia implicita de ADEQUAÇÂO a qualquer
 * MERCADO ou APLICAÇÃO EM PARTICULAR. Veja a
 * Licença Pública Geral GNU para maiores detalhes.
 *
 * Você deve ter recebido uma cópia da Licença Pública Geral GNU
 * junto com este programa, se não, acesse no website oficial:
 * http://www.gnu.org/licenses/gpl.html
 *
 */

import java.util.Vector;

/**
 *
 * @author Diego Schmaedech Martins (schmaedech@gmail.com)
 * @version 29/07/2010
 */
public class StdStats {

    /**
     * Return maximum value in array, -infinity if no such value.
     */
    public static double max(double[] a) {
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < a.length; i++) {
            if (a[i] > max) max = a[i];
        }
        return max;
    }

    public static float max(float[] a) {
        float max = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < a.length; i++) {
            if (a[i] > max)
                max = a[i];
        }
        return max;
    }

    /**
     * Return maximum value of array, Integer.MIN_VALUE if no such value
     */
    public static int max(int[] a) {
        int max = Integer.MIN_VALUE;
        for (int i = 0; i < a.length; i++) {
            if (a[i] > max) max = a[i];
        }
        return max;
    }

    /**
     * Return minimum value in array, +infinity if no such value.
     */
    public static double min(double[] a) {
        double min = Double.POSITIVE_INFINITY;
        for (int i = 0; i < a.length; i++) {
            if (a[i] < min) min = a[i];
        }
        return min;
    }

    /**
     * Return minimum value of array, Integer.MAX_VALUE if no such value
     */
    public static int min(int[] a) {
        int min = Integer.MAX_VALUE;
        for (int i = 0; i < a.length; i++) {
            if (a[i] < min) min = a[i];
        }
        return min;
    }


    /**
     * Return average value in array, NaN if no such value.
     */
    public static double mean(double[] a) {
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            sum = sum + a[i];
        }
        return sum / a.length;
    }
    /**
     * Return average value in array, NaN if no such value.
     */
    public static float mean(float[] a) {
        float sum = 0f;
        for (int i = 0; i < a.length; i++) {
            sum = sum + a[i];
        }
        return sum / a.length;
    }

    /**
     * Return average value in array, NaN if no such value.
     */
    public static double mean(int[] a) {
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            sum = sum + a[i];
        }
        return sum / a.length;
    }

    /**
     * Return sample variance of array, NaN if no such value.
     */
    public static double var(double[] a) {
        if (a.length == 0) throw new RuntimeException("Array size is 0.");
        double avg = mean(a);
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            sum += (a[i] - avg) * (a[i] - avg);
        }
        return sum / (a.length - 1);
    }

    /**
     * Return sample variance of array, NaN if no such value.
     */
    public static double var(int[] a) {
        if (a.length == 0) throw new RuntimeException("Array size is 0.");
        double avg = mean(a);
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            sum += (a[i] - avg) * (a[i] - avg);
        }
        return sum / (a.length - 1);
    }

    /**
     * Return sample variance of array, NaN if no such value.
     */
    public static float var(float[] a) {
        if (a.length == 0) throw new RuntimeException("Array size is 0.");
        float avg = mean(a);
        float sum = 0f;
        for (int i = 0; i < a.length; i++) {
            sum += (a[i] - avg) * (a[i] - avg);
        }
        return sum / (a.length - 1);
    }

    /**
     * Return sample standard deviation of array, NaN if no such value.
     */
    public static double stddev(double[] a) {
        return Math.sqrt(var(a));
    }

    /**
     * Return sample standard deviation of array, NaN if no such value.
     */
    public static float stddev(float[] a) {
        return (float) Math.sqrt(var(a));
    }
    /**
     * Return sample standard deviation of array, NaN if no such value.
     */
    public static double stddev(int[] a) {
        return Math.sqrt(var(a));
    }

    /**
     * Return sum of all values in array.
     */
    public static double sum(double[] a) {
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            sum += a[i];
        }
        return sum;
    }

    /**
     * Return sum of all values in array.
     */
    public static float sum(float[] a) {
        float sum = 0f;
        for (int i = 0; i < a.length; i++) {
            sum += a[i];
        }

        return sum;
    }

    /**
     * Return sum of all values in array.
     */
    public static int sum(int[] a) {
        int sum = 0;
        for (int i = 0; i < a.length; i++) {
            sum += a[i];
        }
        return sum;
    }

    public static double[] getDoubleArray(Vector<Double> vector){
        double[] values = new double[vector.size()];
        int i =0;
        for(double dbl : vector) {
            values[i++] = dbl;
        }
        return values;
    }

    public static float[] getFloatArray(Vector<Float> vector){
        float[] values = new float[vector.size()];
        int i =0;
        for(float flt : vector) {
            values[i++] = flt;
        }
        return values;
    }

    public static float calculeMEAN(Vector<Float> rr){
        return mean(getFloatArray(rr));
    }

    public static float calculeBC(Vector<Float> rr){
        return Math.abs(60000f/rr.lastElement());
    }

    public static float calculeSD(Vector<Float> rr){
        return stddev(getFloatArray(rr));
    }

    public static float calculeSD1(Vector<Float> rr){
        return (float) (Math.sqrt(0.5) * calculeSDSD(rr));
    }

    public static float calculeSD2(Vector<Float> rr){
        return (float) Math.sqrt( 2*Math.pow(calculeSD(rr), 2) - 0.5 * Math.pow(calculeSDSD(rr), 2));
    }

    public static float calculeSDSD(Vector<Float> rr){
        float[] adjacent = new float[rr.size()-1];
        for(int t=0; t < rr.size()-1; t++){
            adjacent[t] = rr.get(t) - rr.get(t+1);
        }
        return stddev(adjacent);
    }

    public static float calculeRMSSD(Vector<Float> rr){
        float[] adjacent = new float[rr.size()-1];
        float[] adjacentPow2 = new float[adjacent.length];

        for(int t=0; t < rr.size()-1; t++){
            adjacent[t] = rr.get(t) - rr.get(t+1);
            adjacentPow2[t] = (float) Math.pow(adjacent[t],2);
        }
        float adj = 0;
        for(int t = 0; t < adjacentPow2.length; t++){
            adj += adjacentPow2[t];
        }
        return (float) Math.sqrt(adj/adjacentPow2.length);
    }

    /*
     * y = ax + b where result[0]=a and result[1] = b
     * and correlation r = result[2]
     */
    public static float[] calculeLinearRegression(float[] x, float[] y){
        float[] result = new float[]{0f,0f,0f};
        int n = x.length;
        float sumX = 0, sumY = 0,sumXY = 0, sumX2 = 0, sumY2 = 0, pow2SumX = 0;

        sumX = sum(x);
        pow2SumX = (float)Math.pow(sumX, 2);
        sumY = sum(y);

        for(int i = 0; i < n; i++){
            sumXY += x[i]*y[i];
            sumX2 += Math.pow(x[i], 2);
            sumY2 += Math.pow(y[i], 2);
        }



        try{
            result[0] = (n*sumXY-sumX*sumY)/(n*sumX2-pow2SumX);
            result[1] = (sumY-result[0]*sumX)/n;
            result[2] = ( n*sumXY-sumX*sumY )/( (float)Math.sqrt( (n*sumX2-pow2SumX)*(n*sumY2-Math.pow(sumY,2)) ) );
        }catch(Exception ex){

        }
        return result;
    }

    /* NON-LINEAR methods
    * m, specifies the pattern length
    * r, defines the criterion of similarity
    * called by chart engine
    */
    public static float calculeApEn(int m, float r, Vector<Float> rr){
        float result = 0;
        result = calculePhy(m,r, rr) - calculePhy(m+1,r, rr);
        return result;
    }

    //variavel para o calculo da entropia aproximada //somatorio do Cm
    private static float calculePhy(int m, float r, Vector<Float> rr){
        float result = 0;
        int N = rr.size();
        int jNm = N - m + 1;
        float[][] Uuj = new float[jNm][m];
        for(int k = 0; k < jNm; k++){
            for(int i = 0; i < m; i++){
                Uuj[k][i] = rr.get(i+k);
            }
        }
        for(int i = 0; i < jNm; i++){
            result += Math.log(calculeCmj(i,Uuj,r,jNm,true));
        }
        result = result/(float)jNm;
        return result;
    }

    /*
     * called by chart engine
     */
    public static float calculeLnSampEn(int m, float r, Vector<Float> rr){
        float result = 0;
        try{
            result = (float) Math.log( ( calculeSampEn(m, r, rr) / calculeSampEn(m+1, r, rr) ) );
        }catch(Exception ex){

        }
        return result;
    }

    //calculo somatorio do Cm
    private static float calculeSampEn(int m, float r, Vector<Float> rr){
        float result = 0;
        int N = rr.size();
        int jNm = N - m + 1;
        float[][] Uuj = new float[jNm][m];
        for(int k = 0; k < jNm; k++){
            for(int i = 0; i < m; i++){
                Uuj[k][i] = rr.get(i+k);
            }
        }
        for(int i = 0; i < jNm; i++){
            result += calculeCmj(i,Uuj,r,jNm,false);
        }
        result = result/(float)jNm;
        return result;
    }

    /*
     *
     *
     */
    public static float calculeD2(int m, int rBin, float[] rr){
        if(rr.length > m){
            float[] x = new float[rBin];
            float[] y = new float[rBin];
            float r = -3f;
            float lnD2 = 0f;
            for(int i = 0; i < rBin; i++){
                r += 0.03f;
                x[i] = r;

                try{
                    lnD2 = calculeLnD2(m, (float)Math.exp(r), rr);
                }catch(Exception ex){

                }
                y[i] = lnD2;
            }

            int indexTempY = indexMax(absDistance(y));
            if(indexTempY > 0){
                float[] tempY = new float[indexTempY];
                // System.out.println("indexTempY "+ indexTempY);
                System.arraycopy(y, 0, tempY, 0, tempY.length);
                int rMin = indexMin( absDistance(tempY) );
                int rMax = indexMax( absDistance(y) );
                int middle =  rMax-rMin;
                //System.out.println("middle "+ middle);
                if(middle > 0){
                    float[] slopeX = new float[middle];
                    float[] slopeY = new float[middle];
                    for(int i = 0; i< middle; i++){
                        slopeX[i] = x[i+rMin];
                        slopeY[i] = y[i+rMin];
                    }
                    return calculeLinearRegression(slopeX, slopeY)[0];
                }
            }
        }
        return 0f;
    }

    /*
     * double lnr = -1.5;
     *   for(int i = 0; i < 50; i++){
     *        lnr += 0.02;
     *        System.out.println( lnr +"\t"+ StdStats.calculeLnD2(10, Math.exp(lnr), rr) );
     *   }
     */
    public static float calculeLnD2(int m, float r, float[] rr){
        float result = 0;

        int jNm = rr.length - m + 1;
        float[][] Uuj = new float[jNm][m];
        for(int k = 0; k < jNm; k++){
            for(int i = 0; i < m; i++){
                Uuj[k][i] = rr[i+k];
            }
        }
        for(int i = 0; i < jNm; i++){
            result += calculeD2Cmj(i,Uuj,r,jNm, false);
        }

        return (float) Math.log(result/(float)jNm);
    }

    private static double calculeD2Cmj(int j, float[][] Uj, float r, int jNm, boolean itself){
        float result = 0;
        int nbrofu = 0;
        for( int k = 0; k < jNm; k++ ){
            if(itself){
                result = dCoUjUk( Uj[j], Uj[k] );
            }else{
                if(j!=k){
                    result = dCoUjUk( Uj[j], Uj[k] );
                }
            }
            if( result <= r ){
                nbrofu++;
            }
        }
        if(itself){
            result = (float)nbrofu/jNm;
        }else{
            result = (float)nbrofu/(jNm-1);
        }
        return result;
    }

    private static float calculeCmj(int j, float[][] Uj, float r, int jNm, boolean itself){
        float result = 0;
        int nbrofu = 0;
        for( int k = 0; k < jNm; k++ ){
            if(itself){
                result = dEnUjUk( Uj[j], Uj[k] );
            }else{
                if(j!=k){
                    result = dEnUjUk( Uj[j], Uj[k] );
                }
            }

            if( result <= r ){
                nbrofu++;
            }
        }
        if(itself){
            result = (float)nbrofu/jNm;
        }else{
            result = (float)nbrofu/(jNm-1);
        }

        return result;
    }

    //definido para ApEn e SamplEn
    private static float dEnUjUk(float[] uj, float[] uk){
        float[] result = new float[uj.length];
        for(int i = 0; i < result.length; i++){
            result[i] = Math.abs(uk[i]-uj[i]);
        }
        return max(result);
    }

    //definido para dimension correlation
    private static float dCoUjUk(float[] uj, float[] uk){
        float result = 0f;
        for(int i = 0; i < uj.length; i++){
            result += Math.pow(uk[i]-uj[i],2);
        }
        return (float) Math.sqrt(result);
    }


    /**
     * Return absolut distance in array, -infinity if no such value.
     */
    public static double[] absDistance(double[] a) {
        double[] result = new double[a.length-1];
        for (int i = 0; i < a.length-1; i++) {
            result[i] = Math.abs(a[i]-a[i+1]);
        }
        return result;
    }

    /**
     * Return absolut distance in array, -infinity if no such value.
     */
    public static float[] absDistance(float[] a) {
        float[] result = new float[a.length-1];
        for (int i = 0; i < a.length-1; i++) {
            result[i] = Math.abs(a[i]-a[i+1]);
        }
        return result;
    }

    public static int indexMax(float[] a) {
        float max = Float.NEGATIVE_INFINITY;
        int index = 0;
        for (int i = 0; i < a.length; i++) {
            if (a[i] > max) {
                max = a[i];
                index = i;
            }
        }
        return index;
    }

    /**
     * Return minimum value in array, +infinity if no such value.
     */
    public static int indexMin(double[] a) {
        double min = Double.POSITIVE_INFINITY;
        int index = 0;
        for (int i = 0; i < a.length; i++) {
            if (a[i] < min){
                min = a[i];
                index = i;
            }
        }
        return index;
    }

    /**
     * Return minimum value in array, +infinity if no such value.
     */
    public static int indexMin(float[] a) {
        float min = Float.POSITIVE_INFINITY;
        int index = 0;
        for (int i = 0; i < a.length; i++) {
            if (a[i] < min){
                min = a[i];
                index = i;
            }
        }
        return index;
    }

    public static float calculeVLF(float[] xs, float[] ys){
        float result = 0;
        for(int i = 0; i < xs.length; i++){
            if(xs[i] <= 0.04f){
                result += ys[i];
            }
        }
        return result;
    }

    public static float calculeLF(float[] xs, float[] ys){
        float result = 0;
        for(int i = 0; i < xs.length; i++){
            if(xs[i] > 0.04f && xs[i] <= 0.15f){
                result += ys[i];
            }
        }
        return result;
    }

    public static float calculeHF(float[] xs, float[] ys){
        float result = 0;
        for(int i = 0; i < xs.length; i++){
            if(xs[i] > 0.15f && xs[i] <= 0.4f){
                result += ys[i];
            }
        }
        return result;
    }

    public static float calculeLFHF(float[] xs, float[] ys){
        float result = 0;
        float hf = calculeHF(xs,ys);
        if(hf>0){
            return calculeLF(xs,ys)/hf;
        }

        return result;
    }

}

