package org.maeden.controller;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import java.util.List;
import java.util.ArrayList;
import java.util.Vector;
import java.io.BufferedReader;
import java.util.StringTokenizer;

/**
 * Simple class for representing 'pre-processed' sensory packets.
 * Agents can bypass the low-level 'raw' sensory data and especially the problem of parsing
 * the contents of the visual field by accessing an 2D array of Lists of world objects.
 *
 * @author: Wayne Iba
 * @version: 2017090901
 */
public class SensoryPacket
{

    public static final String NUMLINES = "8";

    String status;
    String smell;
    List<Character> inventory;
    //List<Character>[][] visualArray = (List<Character>[][])new ArrayList[7][5];
    ArrayList<ArrayList<Vector<String>>> visualArray;
    List<Character> groundContents;
    String messages;
    int energy;
    boolean lastActionStatus;
    int worldTime;
    String[] rawSenseData;
    JSONArray jsonArray;

    /**
     * constructor that reads the raw data from the server via the provided BufferedReader
     * and performs some amount of preprocessing on that raw data.
     */
    public SensoryPacket(BufferedReader gridIn) {
        visualArray = new ArrayList<ArrayList<Vector<String>>>();
        for (int row = 0; row < 7; row++) {
            visualArray.add(row, new ArrayList<Vector<String>>());
            for (int col = 0; col < 5; col++) {
                visualArray.get(row).add(col, new Vector<String>());
            }
        }
        rawSenseData = getRawSenseDataFromGrid(gridIn);
        initPreProcessedFields(rawSenseData);
    }

    /**
     * another constructor takes in the sensory data as parameters instead of using a BufferedReader
     */
    public SensoryPacket(String inptStatus, String inptSmell, List<Character> inptInventory,
                         ArrayList<ArrayList<Vector<String>>> inptVisualArray,
                         List<Character> inptGroundContents, String inptMessages,
                         Integer inptEnergy, Boolean inptLastActionStatus, Integer inptWorldTime) {
        status = inptStatus;
        smell = inptSmell;
        inventory = inptInventory;
        //visualArray = inptVisualArray;
        groundContents = inptGroundContents;
        messages = inptMessages;
        energy = inptEnergy;
        lastActionStatus = inptLastActionStatus;
        worldTime = inptWorldTime;
    }

    /**
     * Just read the raw data into an array of String.  Initialize the status field from line 0
     * <p>
     * LINE0: # of lines to be sent or one of: die, success, or End
     * LINE1: smell (food direction)
     * LINE2: inventory
     * LINE3: visual contents
     * LINE4: ground contents
     * LINE5: messages
     * LINE6: remaining energy
     * LINE7: lastActionStatus
     * LINE8: world time
     *
     * @param gridIn the reader connected to the server
     * @return the array of String representing the raw (unprocessed) sensory data starting with smell
     */
    protected String[] getRawSenseDataFromGrid(BufferedReader gridIn) {
        String[] result = new String[Integer.parseInt(NUMLINES)];
        try {
            JSONParser jsonParser = new JSONParser();
            jsonArray = (JSONArray) jsonParser.parse(gridIn.readLine()); // unpack the JsonArray.
            for (int i = 0 ; i < jsonArray.size() ; i++){
                result[i] = jsonArray.get(i).toString(); // fill the the resultArray with the information.
            }
        } catch (Exception e){
            e.getMessage();
            System.exit(1); // exist if all the elements in the JsonArray are null.
        }
        return result;
    }

    /**
     * Perform any pre-processing, especially on the visual data
     * @param rawSenseData the raw unprocessed sense data
     */
    protected void initPreProcessedFields(String[] rawSenseData){
        try {
            // smell
            this.smell = rawSenseData[0];
            // process inventory
            this.inventory = new ArrayList<Character>();
            for(char item : rawSenseData[1].replaceAll("[\\(\"\\)\\s]+","").toCharArray())
                this.inventory.add(item);
            // visual field
            //processRetinalField((String) jsonArray.get(2));
            // ground contents
            this.groundContents = new ArrayList<Character>();
            for(char item : rawSenseData[3].replaceAll("[\\(\"\\)\\s]+","").toCharArray())
                this.groundContents.add(item);
            // messages: *** Revisit this!! ***
            this.messages = rawSenseData[4];
            // energy
            this.energy = Integer.parseInt(rawSenseData[5]);
            // lastActionStatus
            this.lastActionStatus = rawSenseData[6].equalsIgnoreCase("ok");
            // world Time
            this.worldTime = Integer.parseInt(rawSenseData[7]);
        }catch (NullPointerException e){ e.getMessage(); }
    }

    /**
     * Process the single string representing all the rows and column contents of the visual sensory data
     * and convert it to a 2D array of Vectors of Strings.
     * @param info the visual sensory data string (structered as parenthesized list of lists) from server
     */
    protected void processRetinalField(String info) {
        boolean seeAgent;
        StringTokenizer visTokens = new StringTokenizer(info, "(", true);
        visTokens.nextToken();
        for (int i = 6; i >= 0; i--) {              //iterate backwards so character printout displays correctly
            visTokens.nextToken();
            for (int j=0; j <=4; j++) {             //iterate through the columns
                seeAgent = false;
                int agentID = 0;
                visTokens.nextToken();
                char[] visArray = visTokens.nextToken().replaceAll("[\\(\"\\)\\s]+","").toCharArray();
                for(int k=0; k < visArray.length; k++){
                    if (visArray[k] >= 0 && visArray[k] <= 9){  // we have a digit
                        if (seeAgent){ // we're already processing an agent ID with possibly more than one digit
                            agentID = 10*agentID + (visArray[k] - '0');
                        } else {       // starting to process an agent ID
                            seeAgent = true;
                            agentID = (visArray[k] - '0');
                        }
                    } else {                                    // we have a non-agent ID
                        if (seeAgent){ // just finished processing agent ID -- record it
                            visualArray.get(i).get(j).add(String.valueOf(agentID));
                            seeAgent = false;
                            agentID = 0;
                        }
                        visualArray.get(i).get(j).add(String.valueOf(visArray[k])); // add the non-agent item
                    }
                }
            }
        }
    }

    /** Get the status of the agent in the simulation.  Refer to documentation and/or code
     * for definitive details but either is a number of raw lines to be subsequently processed
     * or is one of "DIE", "END", or "SUCCEED".  This will not typically be used by agents.
     * @return the status of the simulation
     */
    public String getStatus(){ return status; }

    /**
     * @return the string direction toward the food source as one of forward, back, left or right
     */
    public String getSmell(){ return smell; }

    /**
     * @return the current contents of the inventory as a list
     */
    public List<Character> getInventory(){
        //return inventory;
        String open = "(";
        String close = ")";
        String quote = "\"";
        String comma = ",";
        List<Character> finalInv = new ArrayList<Character>();
        finalInv.add(open.charAt(0));
        JSONArray inv = (JSONArray) jsonArray.get(1);
        for(int i = 0 ; i < inv.size() ; i++){
            for(int j = 0 ; j < inv.get(i).toString().length() ; j++){
                String x = inv.get(i).toString();
                finalInv.add(quote.charAt(0));
                finalInv.add(x.charAt(j));
                finalInv.add(quote.charAt(0));
                if(i + 1 < inv.size()){
                    finalInv.add(comma.charAt(0));
                }
            }
        }
        finalInv.add(close.charAt(0));
        return finalInv;
        }

    /**
     * @return the array of lists of strings representing what is currently within the field of view
     */
    public ArrayList<ArrayList<Vector<String>>> getVisualArray(){
        JSONArray visJSON = (JSONArray) jsonArray.get(2);
        for(int i = 0 ; i < visJSON.size() ; i++){
            for(int j = 0 ; j < ((JSONArray) visJSON.get(i)).size(); j++){
                for(int k = 0 ; k < ( (JSONArray) ( (JSONArray) visJSON.get(i)).get(j)).size() ; k++){
                    String x = (String) ( (JSONArray) ( (JSONArray) visJSON.get(i)).get(j)).get(k);
                    visualArray.get(i).get(j).add(x);
                }
            }
        }
        return visualArray; }

    /**
     * @return the list of characters on the ground where the agent is standing
     */
    public List<Character> getGroundContents(){ 
        //return groundContents; 
        List<Character> finalGround = new ArrayList<Character>();
        String open = "(";
        String close = ")";
        String comma = ",";
        String quote = "\"";
        finalGround.add(open.charAt(0));
        JSONArray ground = (JSONArray) jsonArray.get(3);
        for(int i = 0 ; i < ground.size() ; i++){
            for(int j = 0 ; j < ground.get(i).toString().length()-1 ; j++){
                String x = ground.get(i).toString();
                char y = x.charAt(j);
                if (y != quote.charAt(0) & y != comma.charAt(0)){
                    finalGround.add(quote.charAt(0));
                    finalGround.add(y);
                    finalGround.add(quote.charAt(0));
                    if(i + 1 < ground.size()){
                        finalGround.add(comma.charAt(0));
                    }
                }
            }
        }
        finalGround.add(close.charAt(0));
        return finalGround;
        }

    /**
     * NOTE: This may be out of sync with the Grid server and may need to be a list or something else.
     * @return the messages shouted or talked by other agents in the environment
     */
    public String getMessages(){ return messages; }

    /**
     * @return the remaining energy as indicated by the sensory information from the server
     */
    public int getEnergy(){ return energy; }

    /**
     * @return whether the last action was successful (true) or not (false)
     */
    public boolean getLastActionStatus(){ return lastActionStatus; }

    /**
     * @return the world time
     */
    public int getWorldTime(){ return worldTime; }

    /**
     * @return the array of Strings representing the raw sensory data
     */
    public String[] getRawSenseData(){return rawSenseData; }

    /**
     * Renders the visual information as semi-formatted string, making no allowances for
     * cells with more than one object
     */
    public void printVisualArray(){
        for ( ArrayList<Vector<String>> row : visualArray ){
            for ( Vector<String> cell : row ){
                if ( cell != null ){
                    System.out.print('[');
                    for (String s : cell)
                        System.out.print(s);
                    System.out.print(']');
                } else {
                    System.out.print("[ ]");
                }
            }
            System.out.println();
        }
        System.out.println();
    }
    
}
