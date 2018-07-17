package eu.openreq.mulperi.models.json;

import java.util.List;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


/**
* Requirement
* A requirement within the OpenReq framework
* 
*/
public class Requirement {

	/**
	* The unique identifier for a requirement
	* (Required)
	* 
	*/
	@SerializedName("id")
	@Expose
	private String id;
	/**
	* The name of the requirement
	* 
	*/
	@SerializedName("name")
	@Expose
	private String name;
	/**
	* The textual description of the requirement
	* 
	*/
	@SerializedName("text")
	@Expose
	private String text;
	/**
	* The comments to the requirement
	* 
	*/
	@SerializedName("comments")
	@Expose
	private List<Comment> comments = null;
	/**
	* Creation timestamp
	* (Required)
	* 
	*/
	@SerializedName("created_at")
	@Expose
	private int created_at;
	/**
	* Last modification time
	* 
	*/
	@SerializedName("modified_at")
	@Expose
	private int modified_at;
	/**
	* The calculated priority of a requirement
	* 
	*/
	@SerializedName("priority")
	@Expose
	private int priority;
	/**
	* The type of a requirement
	* 
	*/
	@SerializedName("requirement_type")
	@Expose
	private Requirement_type requirement_type;
	/**
	* The current status of a requirement
	* (Required)
	* 
	*/
	@SerializedName("status")
	@Expose
	private Requirement_status status;
	/**
	* The requirements belonging to this requirement
	* 
	*/
	@SerializedName("children")
	@Expose
	private List<String> children = null;
	/**
	* The keywords or tags from the classification of a requirement
	* 
	*/
	@SerializedName("classifierResults")
	@Expose
	private List<Classifier> classifierResults = null;
	
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getText() {
		return text;
	}
	
	public void setText(String text) {
		this.text = text;
	}
	
	public List<Comment> getComments() {
		return comments;
	}
	
	public void setComments(List<Comment> comments) {
		this.comments = comments;
	}
	
	public int getCreated_at() {
		return created_at;
	}
	
	public void setCreated_at(int created_at) {
		this.created_at = created_at;
	}
	
	public int getModified_at() {
		return modified_at;
	}
	
	public void setModified_at(int modified_at) {
		this.modified_at = modified_at;
	}
	
	public int getPriority() {
		return priority;
	}
	
	public void setPriority(int priority) {
		this.priority = priority;
	}
	
	public Requirement_type getRequirement_type() {
		return requirement_type;
	}
	
	public void setRequirement_type(Requirement_type requirement_type) {
		this.requirement_type = requirement_type;
	}
	
	public Requirement_status getStatus() {
		return status;
	}
	
	public void setStatus(Requirement_status status) {
		this.status = status;
	}
	
	public List<String> getChildren() {
		return children;
	}
	
	public void setChildren(List<String> children) {
		this.children = children;
	}
	
	public List<Classifier> getClassifierResults() {
		return classifierResults;
	}
	
	public void setClassifierResults(List<Classifier> classifierResults) {
		this.classifierResults = classifierResults;
	}
}