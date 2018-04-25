import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Evaluation {
    private HashMap<String, ArrayList<String[]>> trecunMap;
    private HashMap<String, String> queriesMap;

    public Evaluation() {
        trecunMap = new HashMap<>();
        queriesMap = new HashMap<>();
    }

    public static void main(String[] args) throws FileNotFoundException {
        //TODO: Im not exactly sure how each map is going to look, this is just starter code.
        Evaluation evaluation = new Evaluation();
        evaluation.readQueries("evaluation-data/queries");
        evaluation.readTrecun("evaluation-data/bm25.trecrun");
        //evaluation.printTrecunMap();
        //evaluation.printQueryMap();
        evaluation.calculateNDCG("african civilian deaths", "1");

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

    //average precision values
    private void calculateNDCG(String query, String Rankposition) {
        if(queriesMap.get(query) == null) {
            throw new Error("Query not found");
        }
        String queryID = queriesMap.get(query);
        System.out.println(queryID);
    }

    private void printTrecunMap() {
        for(Map.Entry entry: trecunMap.entrySet()) {
            String key = (String) entry.getKey();
            ArrayList value = (ArrayList) entry.getValue();
            System.out.print(key + " ");
            for(Object index: value) {
                String[] fixedIndex = (String[]) index;
                System.out.print(Arrays.toString(fixedIndex));
            }
            System.out.print("\n");
        }
    }

    private void printQueryMap() {
        for(Map.Entry entry: queriesMap.entrySet()) {
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            System.out.println(key + ":[" + value + "]");
        }
    }

}
