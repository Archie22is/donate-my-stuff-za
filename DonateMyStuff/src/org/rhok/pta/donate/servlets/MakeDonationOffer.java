package org.rhok.pta.donate.servlets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.net.URLDecoder;
import java.util.Date;
import java.util.UUID;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.rhok.pta.donate.models.DonationOffer;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.labs.repackaged.org.json.JSONException;
import com.google.gson.Gson;
/**
 * Servlet used, by Donoros, to make offers for donation. The offers are stored into the App Engine DataStore.
 * @author Ishmael Makitla
 *
 */
@SuppressWarnings("serial")
public class MakeDonationOffer extends HttpServlet{
	
	private static final Logger log = Logger.getLogger(MakeDonationOffer.class.getSimpleName());
	
	String user = null;
	public void doPost(HttpServletRequest req, HttpServletResponse resp)throws IOException {
		   //get the parameters of the offer
			String payload = req.getParameter("payload");
			log.info("Payload Parameter = "+payload);
			try {
				   if(payload != null){
					   String decodedPayload = URLDecoder.decode(payload, "UTF-8");
					   log.info("Payload Parameter (DECODED) = "+decodedPayload);
				      doOffer(resp,decodedPayload);
				  }
				  else{
					//possibly the StringEntity was used
					//read the stream
					  log.info("Payload Parameter NOT specified - calling: processRawDonationOfferData(...)");
					  processRawDonationOfferData(req,resp);
				  }
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
	}
	/**
	 * For content that was sent with a custom encoding. This method uses the Reader to read the raw content.
	 * @param request
	 * @param resp
	 */
	private void processRawDonationOfferData(HttpServletRequest request, HttpServletResponse resp){
		
		log.info("processRawDonationOfferData(...)");
		String decodedPayload = "";
		StringBuffer rawData = new StringBuffer();
		  String line = null;
		  try {
			  	BufferedReader reader = request.getReader();
			  	while ((line = reader.readLine()) != null){
			  		rawData.append(line);
			  	}
			  	decodedPayload = URLDecoder.decode(rawData.toString(), "UTF-8");
				log.info("processRawDonationOfferData(...):: Payload Parameter (DECODED) = "+decodedPayload);			  
			  		
		  } catch (Exception e) { e.printStackTrace(); }

		  if(rawData.length()>0){
			  try { doOffer(resp, decodedPayload); } 
			  catch (JSONException e) { e.printStackTrace(); }
		  }
		  else{
			  log.severe("The data stream is empty - no data received");
		  }
	}
	
	/**
	 * This method is used to process the incoming offer from the user
	 * @throws JSONException 
	 */
	private void doOffer(HttpServletResponse response, String payload) throws JSONException{		
		log.info("doOffer ()... Payload = \n "+payload+"\n");	
		String requestId = UUID.randomUUID().toString();
        Key donationOffersKey = KeyFactory.createKey("DonationOffer", requestId);
        Entity donationOffer = new Entity("DonationOffer", donationOffersKey);
        
        Date date = new Date();
      
        DonationOffer donationOfferObject = (new Gson()).fromJson(payload, DonationOffer.class);  
        
        if(donationOfferObject !=null){  
        	donationOfferObject.setOfferDate(date);
        	
            donationOffer.setProperty("id", requestId);
        	donationOffer.setProperty("donor_id", donationOfferObject.getDonorId());
        	donationOffer.setProperty("date", donationOfferObject.getOfferDate());   
        	
        	String message = ""; 
        	
        	if(donationOfferObject.getItem()!= null){        		        	
                donationOffer.setProperty("item_name", donationOfferObject.getItem().getName());                
                donationOffer.setProperty("item_size", donationOfferObject.getItem().getSize());
                donationOffer.setProperty("item_age", donationOfferObject.getItem().getAge());
                donationOffer.setProperty("item_age_restriction", donationOfferObject.getItem().getAgeRestriction());
                donationOffer.setProperty("item_gender", donationOfferObject.getItem().getGenderCode());
                donationOffer.setProperty("item_count",donationOfferObject.getQuantity());
                message = "{\"status\": 200}";
        	}
        	        	
        	DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
            //save the offer
            datastore.put(donationOffer);
            
            writeOutput(response,message);
        }
        else{
        	 writeOutput(response,"{\"status\": 500}");
        }
        
	}
	
	/**
	 * This method is used to write the output (JSON)
	 * @param response - response object of the incoming HTTP request
	 * @param output - message to be out-put
	 */
	private void writeOutput(HttpServletResponse response,String output){
		//send back JSON response
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try{
        	Writer outputWriter = response.getWriter();
        	 log.info("Returning :: "+output);
        	outputWriter.write(output);
        }
        catch(IOException ioe){
        	 log.severe("Error returning response to client..."+ioe.getLocalizedMessage());
        }
	}
	
}
