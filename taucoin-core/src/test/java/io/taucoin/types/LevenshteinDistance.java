package io.taucoin.types;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import io.taucoin.util.ByteUtil;
import io.taucoin.util.HashUtil;

public class LevenshteinDistance {
        private char[] a = "mitcmu".toCharArray();
        private char[] b = "mtacnu".toCharArray();
        private int n = a.length;
        private int m = b.length;
        private int minEdist = Integer.MAX_VALUE;
        private void lwstBT(int i,int j,int edist){
            if(i==n || j==m){
                if(j<m) {
                    edist += m-j;
                }
                if(i<n){
                    edist += n-i;
                }
                minEdist = Math.min(edist,minEdist);
                return;
            }
            if(a[i]==b[j]){
                lwstBT(i+1,j+1,edist);
            }else{
                lwstBT(i+1,j,edist+1);//删除a[i]
                lwstBT(i,j+1,edist+1);//在a[i]前面插入b[j]
                lwstBT(i+1,j+1,edist+1);//修改a[i]=b[j]
            }
        }

        public void lwstBT(){
            lwstBT(0,0,0);
        }

    /**
     * 选用编辑代价最小的，并返回该操作代表的操作数
     * @param swap 替换的代价
     * @param insert 插入的代价
     * @param delete 删除的代价
     * @return 0:替换，1：插入，2：删除
     */
    public static int argMin(int swap, int insert, int delete) {
            // 如果替换编辑距离最少，则返回0标识，
        // 即使三种操作距离一样，优先选择替换操作
        if (swap <= insert && swap <= delete) {
            return 0;
        }

        // 如果插入操作编辑最少，返回1标识，如果插入和删除距离一样，优先选择插入
        if (insert < swap && insert <= delete) {
            return 1;
        }

        // 如果删除操作编辑最少，返回2标识
        if (delete < swap && delete < insert) {
            return 2;
        }

        // 其余情况，选择替换操作
        return 0;
    }

    public static String bestSolution1(byte[] source, byte[] target) throws IllegalArgumentException {
        int sourceLength = source.length;
        int targetLength = target.length;
        String result = "";

        // 如果源长度为零，则全插入
        if (sourceLength == 0) {
            for(int i = 0; i < targetLength; i++) {
                result += "i" + target[i];
            }
            return result;
        }
        // 如果目标长度为零，则全删除
        if (targetLength == 0) {
            for(int i = 0; i < sourceLength; i++) {
                result += "b" + source[i];
            }
            return result;
        }

        // 状态转移矩阵
        int[][] dist = new int[sourceLength + 1][targetLength + 1];
        // 操作矩阵
        int[][] operations = new int[sourceLength + 1][targetLength + 1];

        // 初始化，[i, 0]转换到空，需要编辑的距离，也即删除的数量
        for (int i = 0; i < sourceLength + 1; i++) {
            dist[i][0] = i;
            if (i > 0) {
                operations[i][0] = 2;
            }
        }

        // 初始化，空转换到[0, j]，需要编辑的距离，也即增加的数量
        for (int j = 0; j < targetLength + 1; j++) {
            dist[0][j] = j;
            if (j > 0) {
                operations[0][j] = 1;
            }
        }

        // 开始填充状态转移矩阵，第0位为空，所以从1开始有数据，[i, j]为当前子串最小编辑操作
        for (int i = 1; i < sourceLength + 1; i++) {
            for (int j = 1; j < targetLength + 1; j++) {
                // 第i个数据，实际的index需要i-1，替换的代价，相同无需替换，代价为0，不同代价为1
                int cost = source[i - 1] == target[j - 1] ? 0 : 1;
                // [i, j]在[i, j-1]的基础上，最小的编辑操作为增加1
                int insert = dist[i][j - 1] + 1;
                // [i, j]在[i-1, j]的基础上，最小的编辑操作为删除1
                int delete = dist[i - 1][j] + 1;
                // [i, j]在[i-1, j-1]的基础上，最大的编辑操作为1次替换
                int swap = dist[i - 1][j - 1] + cost;

                // 在[i-1, j]， [i, j-1]， [i-1, j-1]三种转换到[i, j]的最小操作中，取最小值
                dist[i][j] = Math.min(Math.min(insert, delete),swap);

                // 选择一种最少编辑的操作
                operations[i][j] = argMin(swap,insert,delete);
            }
        }

        int i = sourceLength;
        int j = targetLength;
        while (0 != dist[i][j]) {
            result = operations[i][j] + result;
            if(0 == operations[i][j]){
                i--;
                j--;
            }
            else if(1 == operations[i][j]){
                j--;
            }
            else if(2 == operations[i][j]){
                i--;
            }
            else{
                i -= 2;
                j -= 2;
            }
        }

        return result;
    }


    public static String bestSolution(String[] data) throws IllegalArgumentException {
        String source = data[0];
        String target = data[1];
        int sourceLength = source.length();
        int targetLength = target.length();
        String result = "";


        if (sourceLength == 0){
            for(int i = 0; i < targetLength; i++){
                result += "i" + target.charAt(i);
            }
            return result;
        }
        if (targetLength == 0){
            for(int i = 0; i < sourceLength; i++){
                result += "b" + source.charAt(i);
            }
            return result;
        }

        int[][] dist = new int[sourceLength + 1][targetLength + 1];
        int[][] operations = new int[sourceLength + 1][targetLength + 1];

        for (int i = 0; i < sourceLength + 1; i++) {
            dist[i][0] = i;
            if (i > 0) {
                operations[i][0] = 2;
            }
        }

        for (int j = 0; j < targetLength + 1; j++) {
            dist[0][j] = j;
            if (j > 0) {
                operations[0][j] = 1;
            }
        }

        for (int i = 1; i < sourceLength + 1; i++) {
            for (int j = 1; j < targetLength + 1; j++) {
                int cost = source.charAt(i - 1) == target.charAt(j - 1) ? 0 : 1;
                int insert = dist[i][j - 1] + 1;
                int delete = dist[i - 1][j] + 1;
                int swap = dist[i - 1][j - 1] + cost;

                dist[i][j] = Math.min(Math.min(insert, delete),swap);
                operations[i][j] = argMin(swap,insert,delete);

                //Calcula si es menor el coste de trasposicion que el obtenido anteriormente
                if (i > 1 && j > 1 && source.charAt(i - 1) == target.charAt(j - 2) && source.charAt(i - 2) == target.charAt(j - 1) && (target.charAt(j-1) != source.charAt(i-1) && target.charAt(j-2) != source.charAt(i-2))) {
                    if(dist[i - 2][j - 2] + cost <= dist[i][j]){
                        operations[i][j] = 3;
                    }
                    dist[i][j] = Math.min(dist[i][j], dist[i - 2][j - 2] + cost);
                }
            }

        }

        System.out.println("MATRIZ DE DISTANCIAS");
        System.out.println("--------------------");
        for(int x = 0; x < source.length()+1; x++){
            for(int y = 0; y < target.length()+1; y++){
                System.out.print(dist[x][y] + "\t");
            }
            System.out.println();
        }

        System.out.println("MATRIZ DE OPERACIONES");
        System.out.println("--------------------");
        for(int x = 0; x < source.length() + 1; x++){
            for(int y = 0; y < target.length() + 1; y++){
                System.out.print(operations[x][y] + "\t");
            }
            System.out.println();
        }

        int i = sourceLength;
        int j = targetLength;
        while (0 != dist[i][j]) {
            if(0 == operations[i][j]){
                result = "s" + source.charAt(i-1) + target.charAt(j-1) + result;
                i--;
                j--;
            }
            else if(1 == operations[i][j]){
                result = "i" + target.charAt(j-1) + result;
                j--;
            }
            else if(2 == operations[i][j]){
                result = "b" + source.charAt(i-1) + result;
                i--;
            }
            else{
                result = "w" + source.charAt(i-2) + source.charAt(i-1) + target.charAt(j-2) + target.charAt(j-1) + result;
                i -= 2;
                j -= 2;
            }
        }

        for(; j > 0; j--) {
            result = "s" + source.charAt(j-1) + target.charAt(j-1) + result;
        }

        /*int i = source.length()-1;
        int j = target.length()-1;

        if(sourceLength == targetLength){
            int contador = targetLength;
            while(contador > 0){
                result = operations[i][j] + result;
                if(operations[i][j].charAt(0) == 's'){
                    i--;
                    j--;
                    contador--;
                }
                else if(operations[i][j].charAt(0) == 'i'){
                    j--;
                    contador--;
                }
                else if(operations[i][j].charAt(0) == 'b'){
                    i--;
                    contador--;
                }
                else{
                    i -= 2;
                    j -= 2;
                    contador -= 2;
                }
            }
        }
        else if(sourceLength > targetLength){
            int contador = sourceLength;
            while(contador > 0){
                if(j > -1){
                    result = operations[i][j] + result;
                    if(operations[i][j].charAt(0) == 's'){
                        i--;
                        j--;
                        contador--;
                    }
                    else if(operations[i][j].charAt(0) == 'i'){
                        j--;
                        contador--;
                    }
                    else if(operations[i][j].charAt(0) == 'b'){
                        i--;
                        contador--;
                    }
                    else{
                        i -= 2;
                        j -= 2;
                        contador -= 2;
                    }
                }
                else{
                    String conversion = "b" + operations[i][0].charAt(1);
                    result = conversion + result;
                    i--;
                    contador--;
                }
            }
        }
        else{
            int contador = targetLength;
            while(contador > 0){
                if(i > -1){
                    result = operations[i][j] + result;
                    if(operations[i][j].charAt(0) == 's'){
                        i--;
                        j--;
                        contador--;
                    }
                    else if(operations[i][j].charAt(0) == 'i'){
                        j--;
                        contador--;
                    }
                    else if(operations[i][j].charAt(0) == 'b'){
                        i--;
                        contador--;
                    }
                    else{
                        i -= 2;
                        j -= 2;
                        contador -= 2;
                    }
                }
                else{
                    String conversion = "i" + operations[0][j].charAt(2);
//                    System.out.println("i:" + j + "op:" + operations[0][j]);
                    result = conversion + result;
                    j--;
                    contador--;
                }
            }
        }*/


        //System.out.println("DISTANCIA DE EDICION : " + dist[sourceLength][targetLength]);
        return result;
    }

        @Test
    public void test() {
            LevenshteinDistance l =  new LevenshteinDistance();
            l.lwstBT();
            System.out.println(l.minEdist);
        }

        @Test
    public void test1() {
            String[] data = {"12AA123", "12AAA123"};
            System.out.println("CADENA RESULTADO: " + bestSolution(data));
        }

//    private void testLWTDistance() {
//        int loop = 5000;
//        int cap = 200;
//        int diffNum = 10;
//        int falsePositive = 0;
//        int falseNegative = 0;
//
//        boolean first = false;
//
//        for (int k = diffNum + 1; k < loop + diffNum + 1; k++) {
//            // 构造哈希列表
//            byte[] target = new byte[cap];
//            byte[] source = new byte[cap];
//
//            for(int i = 0; i < diffNum; i++) {
//                byte[] bytes = ByteUtil.intToBytes(i + k - diffNum);
//                byte[] hash = HashUtil.sha1hash(bytes);
//                target[i] = hash[0];
//            }
//
//            for (int i = 0; i < cap - diffNum; i++) {
//                byte[] bytes = ByteUtil.intToBytes(i + k);
//                byte[] hash = HashUtil.sha1hash(bytes);
//                target[i + diffNum] = hash[0];
//                source[i] = hash[0];
//            }
//
//            for (int i = cap - diffNum; i < cap; i++) {
//                byte[] bytes = ByteUtil.intToBytes(i + k);
//                byte[] hash = HashUtil.sha1hash(bytes);
//                source[i] = hash[0];
//            }
//
//            int sourceLength = source.length;
//            int targetLength = target.length;
//
//            // 状态转移矩阵
//            int[][] dist = new int[sourceLength + 1][targetLength + 1];
//            // 操作矩阵
//            int[][] operations = new int[sourceLength + 1][targetLength + 1];
//
//            // 初始化，[i, 0]转换到空，需要编辑的距离，也即删除的数量
//            for (int i = 0; i < sourceLength + 1; i++) {
//                dist[i][0] = i;
//                if (i > 0) {
//                    operations[i][0] = 2;
//                }
//            }
//
//            // 初始化，空转换到[0, j]，需要编辑的距离，也即增加的数量
//            for (int j = 0; j < targetLength + 1; j++) {
//                dist[0][j] = j;
//                if (j > 0) {
//                    operations[0][j] = 1;
//                }
//            }
//
//            // 开始填充状态转移矩阵，第0位为空，所以从1开始有数据，[i, j]为当前子串最小编辑操作
//            for (int i = 1; i < sourceLength + 1; i++) {
//                for (int j = 1; j < targetLength + 1; j++) {
//                    // 第i个数据，实际的index需要i-1，替换的代价，相同无需替换，代价为0，不同代价为1
//                    int cost = source[i - 1] == target[j - 1] ? 0 : 1;
//                    // [i, j]在[i, j-1]的基础上，最小的编辑操作为增加1
//                    int insert = dist[i][j - 1] + 1;
//                    // [i, j]在[i-1, j]的基础上，最小的编辑操作为删除1
//                    int delete = dist[i - 1][j] + 1;
//                    // [i, j]在[i-1, j-1]的基础上，最大的编辑操作为1次替换
//                    int swap = dist[i - 1][j - 1] + cost;
//
//                    // 在[i-1, j]， [i, j-1]， [i-1, j-1]三种转换到[i, j]的最小操作中，取最小值
//                    dist[i][j] = Math.min(Math.min(insert, delete), swap);
//
//                    // 选择一种最少编辑的操作
//                    operations[i][j] = optCode(swap, insert, delete);
//                }
//            }
//
//            if (!first) {
//                logger.error("Diff Num:{}, Min dist:{}", diffNum, dist[sourceLength][targetLength]);
//                first = true;
//            }
//
//            Set<Integer> missingSet = new HashSet<>();
//            Set<Integer> confirmationSet = new HashSet<>();
//
//            {
//                int i = sourceLength;
//                int j = targetLength;
//                while (0 != dist[i][j]) {
//                    if (0 == operations[i][j]) {
//                        // 如果是替换操作，则将target对应的替换消息加入列表
//                        if (source[i - 1] != target[j - 1]) {
////                        missingMessage.add(messageList.get(j - 1));
//                            missingSet.add(j - 1);
//                        } else {
//                            confirmationSet.add(j - 1);
//                        }
//                        i--;
//                        j--;
//                    } else if (1 == operations[i][j]) {
//                        // 如果是插入操作，则将target对应的插入消息加入列表
////                    missingMessage.add(messageList.get(j-1));
//                        missingSet.add(j - 1);
//                        j--;
//                    } else if (2 == operations[i][j]) {
//                        // 如果是删除操作，可能是对方新消息，忽略
//                        i--;
//                    }
//                }
//
//                for (; j > 0; j--) {
//                    confirmationSet.add(j - 1);
//                }
//            }
//
//            for (int i = 0; i < diffNum; i++) {
//                if (!missingSet.contains(i)) {
//                    falsePositive++;
//                }
//            }
//
//            for (int i = diffNum; i < cap; i++) {
//                if (missingSet.contains(i)) {
//                    falseNegative++;
//                }
////                if (!confirmationSet.contains(i)) {
////                    falseNegative++;
////                }
//            }
//        }
//
////        int total = loop * cap;
//        logger.error("-------------------False positive:{}, rate:{}, false negative:{}, rate:{}", falsePositive, falsePositive * 1.0 / (loop * diffNum), falseNegative, falseNegative * 1.0 / (loop * (cap - diffNum)));
//    }

//    private void testTwoSender() {
//        int loop = 5000;
//        int cap = 200;
//        int diffNum = 3;
//        int falsePositive = 0;
//        int falseNegative = 0;
//
//        boolean first = false;
//
//        for (int k = diffNum + 1; k < loop + diffNum + 1; k++) {
//            // 构造哈希列表
//            byte[] target = new byte[cap];
//            byte[] source = new byte[cap];
//
//            for (int i = 0; i < diffNum; i++) {
//                byte[] bytes = ByteUtil.intToBytes((int)System.currentTimeMillis() + i);
//                byte[] hash = HashUtil.sha1hash(bytes);
//                target[i] = hash[0];
//                source[i] = hash[19];
//            }
//
////            for(int i = 0; i < diffNum; i++) {
////                byte[] bytes = ByteUtil.intToBytes(i + k - diffNum);
////                byte[] hash = HashUtil.sha1hash(bytes);
////                target[i] = hash[0];
////            }
////
////            for(int i = 0; i < diffNum; i++) {
////                byte[] bytes = ByteUtil.intToBytes(i + 200 * k - diffNum);
////                byte[] hash = HashUtil.sha1hash(bytes);
////                source[i] = hash[0];
////            }
//
//            for (int i = 0; i < cap - diffNum; i++) {
//                byte[] bytes = ByteUtil.intToBytes(i + k);
//                byte[] hash = HashUtil.sha1hash(bytes);
//                target[i + diffNum] = hash[0];
//                source[i + diffNum] = hash[0];
//            }
//
////            for (int i = cap - diffNum; i < cap; i++) {
////                byte[] bytes = ByteUtil.intToBytes(i + k);
////                byte[] hash = HashUtil.sha1hash(bytes);
////                source[i] = hash[0];
////            }
//
//            int sourceLength = source.length;
//            int targetLength = target.length;
//
//            // 状态转移矩阵
//            int[][] dist = new int[sourceLength + 1][targetLength + 1];
//            // 操作矩阵
//            int[][] operations = new int[sourceLength + 1][targetLength + 1];
//
//            // 初始化，[i, 0]转换到空，需要编辑的距离，也即删除的数量
//            for (int i = 0; i < sourceLength + 1; i++) {
//                dist[i][0] = i;
//                if (i > 0) {
//                    operations[i][0] = 2;
//                }
//            }
//
//            // 初始化，空转换到[0, j]，需要编辑的距离，也即增加的数量
//            for (int j = 0; j < targetLength + 1; j++) {
//                dist[0][j] = j;
//                if (j > 0) {
//                    operations[0][j] = 1;
//                }
//            }
//
//            // 开始填充状态转移矩阵，第0位为空，所以从1开始有数据，[i, j]为当前子串最小编辑操作
//            for (int i = 1; i < sourceLength + 1; i++) {
//                for (int j = 1; j < targetLength + 1; j++) {
//                    // 第i个数据，实际的index需要i-1，替换的代价，相同无需替换，代价为0，不同代价为1
//                    int cost = source[i - 1] == target[j - 1] ? 0 : 1;
//                    // [i, j]在[i, j-1]的基础上，最小的编辑操作为增加1
//                    int insert = dist[i][j - 1] + 1;
//                    // [i, j]在[i-1, j]的基础上，最小的编辑操作为删除1
//                    int delete = dist[i - 1][j] + 1;
//                    // [i, j]在[i-1, j-1]的基础上，最大的编辑操作为1次替换
//                    int swap = dist[i - 1][j - 1] + cost;
//
//                    // 在[i-1, j]， [i, j-1]， [i-1, j-1]三种转换到[i, j]的最小操作中，取最小值
//                    dist[i][j] = Math.min(Math.min(insert, delete), swap);
//
//                    // 选择一种最少编辑的操作
//                    operations[i][j] = optCode(swap, insert, delete);
//                }
//            }
//
//            if (!first) {
//                logger.error("Diff Num:{}, Min dist:{}", diffNum, dist[sourceLength][targetLength]);
//                first = true;
//            }
//
//            Set<Integer> missingSet = new HashSet<>();
//            Set<Integer> confirmationSet = new HashSet<>();
//
//            {
//                int i = sourceLength;
//                int j = targetLength;
//                while (0 != dist[i][j]) {
//                    if (0 == operations[i][j]) {
//                        // 如果是替换操作，则将target对应的替换消息加入列表
//                        if (source[i - 1] != target[j - 1]) {
////                        missingMessage.add(messageList.get(j - 1));
//                            missingSet.add(j - 1);
//                        } else {
//                            confirmationSet.add(j - 1);
//                        }
//                        i--;
//                        j--;
//                    } else if (1 == operations[i][j]) {
//                        // 如果是插入操作，则将target对应的插入消息加入列表
////                    missingMessage.add(messageList.get(j-1));
//                        missingSet.add(j - 1);
//                        j--;
//                    } else if (2 == operations[i][j]) {
//                        // 如果是删除操作，可能是对方新消息，忽略
//                        i--;
//                    }
//                }
//
//                for (; j > 0; j--) {
//                    confirmationSet.add(j - 1);
//                }
//            }
//
//            for (int i = 0; i < diffNum; i++) {
//                if (!missingSet.contains(i)) {
////                    logger.error("----False positive index:{}, target:{}, source:{}", i, Hex.toHexString(target), Hex.toHexString(source));
//                    falsePositive++;
//                }
//            }
//
//            for (int i = diffNum; i < cap; i++) {
//                if (missingSet.contains(i)) {
//                    falseNegative++;
//                }
////                if (!confirmationSet.contains(i)) {
////                    falseNegative++;
////                }
//            }
//        }
//
////        int total = loop * cap;
//        logger.error("-------------------False positive:{}, rate:{}, false negative:{}, rate:{}", falsePositive, falsePositive * 1.0 / (loop * diffNum), falseNegative, falseNegative * 1.0 / (loop * (cap - diffNum)));
//    }

}
