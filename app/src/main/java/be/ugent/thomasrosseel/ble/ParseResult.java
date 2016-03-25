package be.ugent.thomasrosseel.ble;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by thomasrosseel on 1/03/16.
 */
public class ParseResult {

    private String id;
    private String title;
    private List<ParseResult> children;

    public ParseResult() {
        children = new ArrayList<>();
    }

    public ParseResult(String id, String title) {
        children = new ArrayList<>();
        this.id = id;
        this.title = title;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void addChild(ParseResult p){
        children.add(p);
    }

    public List<ParseResult> getChildren() {
        return children;
    }

    public boolean contains(String id){

        for(ParseResult child : children){
            if(child.getId().equals("id")){
                return true;
            }
        }
        return  false;

    }

    public ParseResult getChild(String id){
        for(ParseResult child : children){
            if(child.getId().equals(id)){
                return child;
            }
        }
        return  null;

    }
}
