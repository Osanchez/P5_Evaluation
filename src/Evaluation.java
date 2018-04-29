import java.io.*;
import java.util.*;

public class Evaluation {
    private HashMap<String, ArrayList<String[]>> trecunMap;
    private HashMap<String, String> queriesMap;
    private HashMap<String, HashMap<String, String>> relevanceMap;

    public Evaluation() {
        trecunMap = new HashMap<>();
        queriesMap = new HashMap<>();
        relevanceMap = new HashMap<>();
    }

    public static void main(String[] args) throws IOException {
        Evaluation evaluation = new Evaluation();
        evaluation.readQueries("evaluation-data/queries");
        evaluation.readTrecun("evaluation-data/bm25.trecrun");
        evaluation.readRelevance("evaluation-data/qrels");

        //NDCG@15 bm25
        double averageNDCG = 0.00;
        for(Map.Entry entry: evaluation.queriesMap.entrySet()) {
            averageNDCG += evaluation.calculateNDCG(entry.getKey().toString(), 15);
        }
        averageNDCG = averageNDCG/evaluation.queriesMap.size();
        System.out.println("bm25.trecrun NDCG@15 " + averageNDCG);

        //evaluation.printTrecunMap();
        //evaluation.printQueryMap();
        //evaluation.printRelevance();
    }

    private void readQueries(String queryFile) {
        try {
            FileReader fileReader = new FileReader(queryFile);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line;

            while((line = bufferedReader.readLine()) != null) {
                //read each line in the trecrun file and split it by column
                String[] splitLine = line.split("\\s+");
                String queryID = splitLine[0];
                StringBuilder query = new StringBuilder();
                for(int x = 1; x < splitLine.length; x++) {
                    if(x < splitLine.length - 1) {
                        query.append(splitLine[x]).append(" ");
                    } else {
                        query.append(splitLine[x]);
                    }
                }
                //add values to hashmap using queryID as key
                queriesMap.put(query.toString(), queryID);
            }
            bufferedReader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void readTrecun(String fileName) throws FileNotFoundException {
        try {
            FileReader fileReader = new FileReader(fileName);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line;

            while((line = bufferedReader.readLine()) != null) {
                //read each line in the trecrun file and split it by column
                String[] splitLine = line.split("\\s+");
                String queryID = splitLine[0];
                String unusedSkip = splitLine[1]; //not used
                String documentIdentifier = splitLine[2];
                String rank = splitLine[3];
                String retrievalModelScore = splitLine[4];
                String retrievalModel = splitLine[5];

                //add values to hashmap using queryID as key
                String[] mapValues = {rank, documentIdentifier, retrievalModelScore, retrievalModel};
                if(trecunMap.containsKey(queryID)) {
                    trecunMap.get(queryID).add(mapValues);
                } else {
                    ArrayList<String[]> newValue = new ArrayList<>();
                    newValue.add(mapValues);
                    trecunMap.put(queryID, newValue);
                }
            }
            bufferedReader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void readRelevance(String fileName) throws FileNotFoundException {
        try {
            FileReader fileReader = new FileReader(fileName);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line;

            while((line = bufferedReader.readLine()) != null) {
                //read each line in the trecrun file and split it by column
                String[] splitLine = line.split("\\s+");
                String queryID = splitLine[0];
                String unused = splitLine[1]; //not used
                String documentIdentifier = splitLine[2];
                String relevance = splitLine[3];

                //add values to hashmap using queryID as key
                if(relevanceMap.containsKey(queryID)) {
                    relevanceMap.get(queryID).put(documentIdentifier, relevance);
                } else {
                    HashMap<String, String> newRelevance = new HashMap<>();
                    newRelevance.put(documentIdentifier, relevance);
                    relevanceMap.put(queryID, newRelevance);
                }
            }
            //sort the relevance map.
            bufferedReader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //average precision values
    private double calculateNDCG(String query, int rankPosition) {
        if(queriesMap.get(query) == null) {
            throw new Error("Query not found");
        }
        double dcg = 0.00;
        double idcg = 0.00;
        double ndcg = 0.00;

        String queryID = queriesMap.get(query);
        ArrayList queryRankingMap = trecunMap.get(queryID);

        if(queryRankingMap.size() >= rankPosition) {
            //DCG Calculation
            for (int x = 1; x <= rankPosition; x++) {
                String[] rankList = (String[]) queryRankingMap.get(x - 1);
                String relevanceDocument = rankList[1];
                double relevance = 0.00;
                if(relevanceMap.get(queryID).get(relevanceDocument) != null) {
                    relevance = Double.parseDouble(relevanceMap.get(queryID).get(relevanceDocument));
                }
                double numer = (Math.pow(2, relevance)) - 1;
                double denom = log2(x + 1);
                double result = numer / denom;
                dcg += result;
            }

            //IDCG Calculation
            Comparator<Map.Entry<String, String>> valueComparator = (o1, o2) -> {
                String v1 = o1.getValue();
                String v2 = o2.getValue();
                return v1.compareTo(v2);
            };
            List<Map.Entry<String, String>> unsortedRelevanceMap = new ArrayList<>(relevanceMap.get(queryID).entrySet()); //documentID, Relevance
            Collections.sort(unsortedRelevanceMap, valueComparator);
            Collections.reverse(unsortedRelevanceMap);

            LinkedHashMap<String, String> sortedRelevanceMap = new LinkedHashMap<>(unsortedRelevanceMap.size());
            for(Map.Entry<String, String> entry : unsortedRelevanceMap){
                sortedRelevanceMap.put(entry.getKey(), entry.getValue());
            }

            int count = 1;
            for (HashMap.Entry rank : sortedRelevanceMap.entrySet()) {
                if(count == rankPosition) {
                    break;
                }
                double numer = Math.pow(2, Integer.parseInt(rank.getValue().toString())) - 1;
                double denom = log2(count + 1);
                double result = numer / denom;
                idcg += result;
                count++;
            }
            ndcg = dcg/idcg;
        }
        //final result
        //System.out.print("Query: " + query + "\nEvaluation: " + ndcg + "\n\n");
        return ndcg;
    }

    private void printTrecunMap() throws IOException {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter("trecrun.txt"));
            for (Map.Entry entry : trecunMap.entrySet()) {
                String key = (String) entry.getKey();
                ArrayList value = (ArrayList) entry.getValue();
                System.out.print(key + " ");
                for (Object index : value) {
                    String[] fixedIndex = (String[]) index;
                    System.out.print(Arrays.toString(fixedIndex));
                    writer.write(key + " " + Arrays.toString(fixedIndex));
                    writer.newLine();
                }
                System.out.print("\n\n");
            }
            writer.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    private void printQueryMap() {
        for(Map.Entry entry: queriesMap.entrySet()) {
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            System.out.println(key + ":[" + value + "]");
        }
    }

    private void printRelevance() {
        for(Map.Entry entry: relevanceMap.entrySet()) {
            String queryID = entry.getKey().toString();
            System.out.println("Query ID: " + queryID);
            HashMap<String, String> value = (HashMap) entry.getValue();
            for(Map.Entry valueEntry: value.entrySet()) {
               String documentID = valueEntry.getKey().toString();
               String relevance = valueEntry.getValue().toString();
               System.out.println("\t" + "DocumentID: " + documentID + " Relevance: " + relevance);
            }
        }
        System.out.println();
    }

    private  double log2(double num) {
        return (Math.log(num)/Math.log(2));
    }
}
