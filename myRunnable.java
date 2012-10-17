package yelpbot;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.ProxyConfig;
import java.io.IOException;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.ElementNotFoundException;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.DomText;
import java.util.*;
import java.net.MalformedURLException;
import com.gargoylesoftware.htmlunit.WaitingRefreshHandler;


public class myRunnable implements Runnable {    
    
private Zip zip;
private Proxy proxyObj;
private List<String> missedLinks = new ArrayList<String>();
private List<String> zipsNoBars = new ArrayList<String>();
private List<String> barData = new ArrayList<String>();	
private WebClient vBrowser;
private HtmlPage page;
private int totalPages = 0;
private int pagesLessThanTen;
private String nextURL;
private boolean zipSuccess=false;
private boolean noBarResults=false;
private boolean portInUse=false;
private int totalBarsInZip=0;
List<String> barList =  new ArrayList<String>();  
    

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@


    public myRunnable()
    {        
        vBrowser = new WebClient();  
	vBrowser.setThrowExceptionOnScriptError(false);
        vBrowser.setRefreshHandler(new WaitingRefreshHandler());;
    }//end constructor()

    
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    
    
    @Override
    public void run() 
    { 
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        setNextZip();
	setNextProxy();
       
        
        outerLoop:
        while(zip != null && proxyObj != null) //outer loop
        {
            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
             zipSuccess=false; //for checking if entire zip was successfully parsed
             noBarResults=false;     
             if(!issetAliveProxy()&& ProxyList.isOutOfProxies() && proxyObj==null)
	     {
		 System.err.println("OUT OF PROXIES!!!!!!!!!!");
                 setNextZip();
		 break outerLoop;    
	     }//end if
 
                
	     try // 2.
	     {
		
		 boolean gotURL = getURL(); // get URL to nightlife zip code page
		 if(gotURL) // if we succcessfully got the page
		 {                    
		     if(getLinks()==true)
                     {
			 barData.add("^@"+zip.toString()+"^@");
			 
			 parseLoop:
			 for(String barLink : barList) // for each link in the list
			 {
                                boolean notDetected=true;
                                //while(notDetected)
                                notDetected = getSuccessParseLink(barLink);
                                while(!notDetected)
                                {
                                    ProxyList.delete(proxyObj);                 
                                    setNextProxy();
                                    issetAliveProxy();
                                    if(ProxyList.isOutOfProxies())
                                    {
                                        System.out.println("Ran out of proxies while parsing!!!!!!!!!!!");
                                        break parseLoop;
                                    }
                                    notDetected = getSuccessParseLink(barLink);
                                }
				zipSuccess=true;
                                
			 }//end for			 

			//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~			                            
                         
		     }//end if
                     else // we got no links
		     {   
			 System.out.println("List of Bar Links was Empty for zipCode: " + zip.toString());                        
			 zipSuccess=false;			 
		     }//end else                             
		 }//end if
                 else // couldn't access nightlife page through getURL()
		 {           
	             System.out.println("The method getURL() returned false for zip code: " + zip.toString());
                     zipSuccess=false;    
		 }//end else                                                  

		 //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	     }//end try 2.
	     catch(IOException e)
	     {
		 //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		 System.out.println("missed zip: "+zip.toString() + " because of a IOExc: "+e);           
		 ProxyList.delete(proxyObj);                 
		 setNextProxy();
                 continue;
	     }//end
	     //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            
            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	    if(zipSuccess==true || noBarResults==true)
            {		    
		zip.deleteZip();    
                setNextZip();
                setNextProxy();
                    
            }//end if
            else
             {                 
                 ProxyList.delete(proxyObj);
                 setNextProxy();
                 System.out.println("Proxy obj was no good, moving to next one");
             }//end else
            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        }//end outer while
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        
        addMissedLinks();
        
        addBarData();
        

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    }//end run()
    
    public boolean getSuccessParseLink(String barLink)
    {
        
        try
        {
        String barRecord = parseLink(barLink);// go get the bar attributes

        if(barRecord.compareTo("#BadProxy#") == 0)
        {
                System.out.println("Proxy went bad in parseLink in " + zip.toString() + " going to go get new proxy.");                
                return false;
        }//end if
        if(barRecord.compareTo("#NoBueno#") == 0)
        {
                missedLinks.add(barLink);
        }//end if
        else
        {
                barData.add(barRecord);
        }//end else
        }//end try
        catch(java.io.IOException e)
        {
            return false;
        }//end catch
        
        return true;
    }//end getSuccessParseLink
    
    public boolean issetAliveProxy()
    {
        boolean proxyUseable = false;
        while(!proxyUseable && !ProxyList.isOutOfProxies() && proxyObj != null)
            {
                
                try // 1.
                {
                    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    
                        System.out.println("trying proxy...");
                        proxyUseable = tryProxy(proxyObj);
                        System.out.println("Tried the proxy: " + proxyObj.toString());
                    
                    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    
                    if(!proxyUseable && proxyObj != null)
                    {
                        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                        ProxyList.delete(proxyObj);
                        setNextProxy();                       
                        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    }//end if
                    
                    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                }//end try 1.
                catch(IOException e) // 1.
                {
                    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    System.out.println("Caught IOException and deleted proxy: " + proxyObj.toString()+" because: "+e);
                    ProxyList.delete(proxyObj);
                    setNextProxy();
                    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                }// end catch 1.        
        }//end while
        return proxyUseable;
    }//end getAliveProxy
    
    
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    
    public boolean tryProxy(Proxy proxyObj) throws IOException
	{
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                
		boolean connected = false;
                portInUse = false;		                
      
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		ProxyConfig pConfig = new ProxyConfig();	    
		pConfig.setProxyHost(proxyObj.getIP());	    
	        pConfig.setProxyPort(proxyObj.getPort());
                vBrowser.setProxyConfig(pConfig);
                
                page=null;		
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

                     long begTime = getTime();                     
                        
                             try{//3
                             page = (HtmlPage) vBrowser.getPage("http://www.yelp.com/search?find_desc=&find_loc="+zip.toString()+"&ns=1");
                             
                             }//try 3
                             catch(IOException e)
                             {
                             page=null;
                             System.out.println("Connection refused");                             
                             }//end catch
                             catch(FailingHttpStatusCodeException e)
                             {
                                 System.out.println("FailingHttpStatusCodeException in try proxy: "+e);
                                 page=null;                                 
                             }//end catch
                            
                     
                     if(page==null)
                     {
                         connected=false;                         
                         System.out.println("Proxy didn't connect");
                     }//end if
                                         
                     long endTime = getTime();
                     
                     long conTime = endTime - begTime;
                     if(page!=null && conTime<=120000)
                     {
                     System.out.println("Proxy Set! Took: "+conTime+" ms");
		     connected = true;                     
                     }//end if		     

		return connected;
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		
	}//end tryProxy()
    
    
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    
    
    public synchronized void setNextZip()
    {
	    zip =  ZipList.getNext();          
    }//end setNextZip()
    
    
    
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    
    
    public synchronized void setNextProxy()
    {
	    
	    proxyObj = ProxyList.getNext();
            if(proxyObj!=null)
            {
	    
            
            if(!ProxyList.isOutOfProxies())
            {
	    while(!proxyObj.isUseable()&&!ProxyList.isOutOfProxies() && proxyObj!=null)
	    {			    
                proxyObj = ProxyList.getNext();
                if(proxyObj==null)
                {
                    break;
                }//end if
	    }//end while 
            }//end if
            }//end if   
	    
    }//end setNextProxy()
    
    
    
    public synchronized void addMissedLinks()
    {
        DataHandler.addMissedLinks(missedLinks);
        if(zipsNoBars.size()>=1)
        {
        DataHandler.addMissedZips(zipsNoBars);
        }//end if
    }//end getMissedZips()
    
    
    
    public synchronized void addBarData()
    {
        DataHandler.addBarData(barData);
        System.out.println("add bar data method executed");
    }//end getMissedZips()
    
    
    
    
    
    
    
    public boolean getURL() throws IOException, ElementNotFoundException
        {
            boolean gotURL = false;
	    String totalResults = "";
            totalPages = 0;
	    
                                                                      
                                         //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                                         try
                                         {
                                         try
                                         {
                                         page = (HtmlPage)page.getAnchorByText("Nightlife").openLinkInNewWindow();
                                         }//end try
                                         catch(FailingHttpStatusCodeException e)
                                         {
                                             System.out.println("You suck dick : "+e);
                                             return false;
                                         }//end catch
                                         if(page == null)
                                         {
                                            System.out.println("Unable to open NightLife link for zip:  " + zip.toString());
                                            //currentProxy.delete();
                                            return false;
                                         }//end if
                                                 
                                         if(noBarResults())
                                         {
                                                 System.out.println("3-bar: THere are no nightlife results for this zip code...: " + zip.toString());
                                                 noBarResults = true;
                                                 return false;
                                         }//end if
                                         if(!locationInNightLife())
                                         {
                                             System.out.println("Couldn't get into nightlife link for some reason, will move to next proxy");
                                             return false;
                                         }//end if
                                         try
                                         {
//                                             while(vBrowser.waitForBackgroundJavaScript(20000)!=0)
//                                         {
//                                             System.out.println("Waiting on jscript");
//                                         }
                                         totalResults = ((DomText) page.getFirstByXPath("//strong[@class='pager_total']/text()")).asText();
                                         if(noResults())
                                         {
                                                 System.out.println("No of Results not found for zip: " + zip.toString());
                                                 return false;
                                         }//end if
                                         
                                         System.out.println("Number of bars in zip code: " + zip.toString() + " is " + totalResults);
                                         totalBarsInZip = Integer.parseInt(totalResults);
					 
                                         if(0 < Integer.parseInt(totalResults) && Integer.parseInt(totalResults) <= 10 )
                                         {
                                            totalPages = 1;
                                            pagesLessThanTen = Integer.parseInt(totalResults);
                                         }
                                         if(Integer.parseInt(totalResults) > 10)
                                         {
                                            totalPages = (Integer.parseInt(totalResults)/10)+1;
                                            pagesLessThanTen = Integer.parseInt(totalResults)%10;
                                         }//end else
                                         System.out.println("Number of pages in zip code: " + zip.toString() + " is " + totalPages);
                                         if(totalPages > 0)
                                         {					//this section we return true if # of results
                                                 gotURL = true;          //are not found, else divide for # of pages.
                                                 nextURL = page.getUrl().toString();
                                                 System.out.println("URL to get links with is: " + nextURL.toString());
                                         }//end if                                   			     

                                         //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                                         }//end try4
                                         catch(NullPointerException e)
                                         {
                                             System.out.println("Number of pages pointer could not be found" +e);
                                         }//end catch
                                         
                                         catch(FailingHttpStatusCodeException e)
                                         {
                                             System.out.println("Service became unavailable :"+e);
                                         }//end catch
                                     }//end try
                                         catch(NullPointerException e)
                                         {
                                             System.out.println("Null pointer at nightlife link: " +e);
                                             return false;
                                         }//end catch
                                         catch(ElementNotFoundException e)
                                         {
                                             System.out.println("Element not found: "+e);
                                         }//end catch
                                         
                                        
                                     //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                                     
                                     //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			     
                    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            vBrowser.closeAllWindows();
            return gotURL;
        }//end getLink
    
     private boolean noResults() // this method tests if yelp has served an error, this occurs if yelp has allowed access of the current proxy -
                                    // but has blocked services to it based on knowing its a proxy.
        {            
            try //try
             {             
             List<DomNode> errorExists = (List<DomNode>) page.getByXPath("//div[@id='error_result']");             
             if(errorExists.size()>=1)
             {
                 System.out.println("Yelp served an error for this zip:" +zip.toString());
                 vBrowser.closeAllWindows();
                 return true;
             }//end if
             else
             {
                 //System.out.println("Yelp returned the nightlife links for this zip: " +zip);
                 return false;
             }//end else
             }//end try ErrorResult
             catch(NullPointerException e)
             {
                 //System.out.println("Yelp returned the nightlife links for this zip: " +zip);
                 return false;
             }//end catch
        }//end noResults
     
     
     private boolean locationInNightLife() // this method tests if we are in the nightlife area of the website.
        {            
            try //try
             {             
             List<DomNode> inNightLife = (List<DomNode>) page.getByXPath("/html/body/div[2]/div[3]/div[3]/div/div/p/em");             
             if(inNightLife.size()>=1)
             {
                 System.out.println("Got into nightlife on this zip: " +zip.toString());                 
                 return true;
             }//end if
             else
             {
                 return false;
             }//end else
             }//end try ErrorResult
             catch(NullPointerException e)
             {
                 return false;
             }//end catch
        }//end noResults
     
     
     
     
     private boolean noBarResults()
     {
         noBarResults = false;
         try
         {
            
         
         List<DomNode> errorExists1 = (List<DomNode>) page.getByXPath("//div[@id='error_result']");
         List<DomNode> errorExists = (List<DomNode>) page.getByXPath("/html/body/div[2]/div[3]/div[3]/div[2]/div/div[2]/div/p");
         if(errorExists.size()>=1 && errorExists1.size()>=1)
         {
             System.out.println("Yelp has no nightlife results for this zip: "+zip.toString());            
             return true;
         }//end if
         else
         {
             return false;
         }//end else
         }//end try
         catch(NullPointerException e)
         {
             return false;
         }//end catch
     }//end noBarResults
     
             
     
     
     public boolean getLinks() throws IOException
	{
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~                
		
                
		
                boolean gotTheLinks = true;
                barList.clear();
		missedLinks = new ArrayList<String>();
		int linkCount = 0;
                int tracker = 0;
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		outerloop:
		for(int i = 0; i < totalPages; i++) //outer for
		{		  
		  if(i==totalPages-1)
                  {
                      tracker = pagesLessThanTen;
                  }//end if
                  else
                  {
                      tracker=10;
                  }//end else
                    for(int j = 0; j < tracker; j++) //inner for
		  {
                        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			try//try 0
                        {                      
			String temp = page.getElementById("bizTitleLink" + linkCount).getAttribute("href");
                        
                        if(temp != null)                           
			{
			  //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			  try //1.
			  {		            
                            temp = page.getAnchorByHref(temp).getHrefAttribute(); 
                            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			    try // 2.
			    {				
				temp = page.getFullyQualifiedUrl(temp).toString();
				barList.add(temp);
				linkCount++;
                            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			    }//end try 2.
			    catch(MalformedURLException e) // 2.
			    {
				 //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
				 System.out.println("Could not get URL from link: " + linkCount + ". Error: " + e);
				 missedLinks.add(zip.toString() + "     " + linkCount + "\n");				 
			    }// end catch 2.
                            
			  //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			  }//end try 1.                        
                          catch(ElementNotFoundException e) //1.
                          {
		            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~		  
			    System.out.println("Could not grab link: " + linkCount + ". Error: " + e);
			    missedLinks.add(zip.toString() + "     " + linkCount + "\n");			    
		          }//end catch 1.
                        
			  //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			}//end if               
			else
			{
			  //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			  System.out.println("Could not grab link: " + linkCount + ". Link returned null.");
			  missedLinks.add(zip.toString() + "     " + linkCount + "\n");			  
			}//end else
                        
                        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~                 
                        }//end try 0
                        catch(NullPointerException e)
                        {
                            System.out.println("NullPointer error: "+e);
                        }//end catch			
		  
		  }//end inner for
		  //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                if(i == totalPages-1)
                {
                    break;
                }//end if
                
                try //1
		{
		    try //2
		    {   if(page.getAnchorByText("Next") != null)
			{
			try //3
                        {
                            page = (HtmlPage)page.getAnchorByText("Next").openLinkInNewWindow();
                        }//end try 3
                        catch(FailingHttpStatusCodeException e)
                        {
                            System.out.println("Proxy Shit out while accumulating bar hrefs error: "+e);
                            vBrowser.closeAllWindows();
                            gotTheLinks = false;
                            return gotTheLinks;
                        }//end catch //3
                                if(noResults())
                                {
                                    System.out.println("5.Most likely Proxy is blocked on this zip...: " + zip.toString());                                                                        
                                    gotTheLinks = false;
                                    return gotTheLinks;
                                }//end if
			}//end if			
                    
                    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		    }//end try 2
		    catch(NullPointerException ee)  //If ((page = .... Cannot copypaste on mobile)!=null)
		    {
			System.out.println("NullPointer :: "+ee);
		    }//end catch
                    
                //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		}//end try 1
		catch(ElementNotFoundException e)
		{
		    System.out.println("ElementNotFound :: "+e);
		}//end catch

	        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~	
                
		}//end outer for
                
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                vBrowser.closeAllWindows();
		return gotTheLinks;
                
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		
	}//end getLinks()
	
     
     public String parseLink(String barLink) throws IOException
	{
		
		
		List<DomNode> checkExsist;// List of DomNodes for checking if Attribute exsists on page
                String barRecord="";               
                
		try
		{
		try	
		{
                page = (HtmlPage) vBrowser.getPage(barLink);                
                

                checkExsist = (List<DomNode>) page.getByXPath("//h1[@class='fn org']");
                if(checkExsist.size()>=1)
                {
                barRecord+="~*"+ ((DomText) page.getFirstByXPath("//h1[@class='fn org']/text()")).asText();
                }//endif
		else
		{
		     barRecord+="~*#NULL#";
		}//end else

                checkExsist = (List<DomNode>) page.getByXPath("//span[@class='street-address']");
                if(checkExsist.size()>=1)
                {
                barRecord+="~*"+ ((DomText) page.getFirstByXPath("//span[@class='street-address']/text()")).asText();
                }//end
		else
		{
		     barRecord+="~*#NULL#";
		}//end else
                
		checkExsist = (List<DomNode>) page.getByXPath("//span[@class='locality']");
                if(checkExsist.size()>=1)
                {
                barRecord+="~*"+((DomText) page.getFirstByXPath("//span[@class='locality']/text()")).asText();
                }//endif
		else
		{
		     barRecord+="~*#NULL#";
		}//end else
		

                checkExsist = (List<DomNode>) page.getByXPath("//span[@class='region']");
                if(checkExsist.size()>=1)
                {
                barRecord+="~*"+ ((DomText) page.getFirstByXPath("//span[@class='region']/text()")).asText();
                }//end if
		else
		{
		     barRecord+="~*#NULL#";
		}//end else

                checkExsist = (List<DomNode>) page.getByXPath("//span[@class='postal-code']");
                if(checkExsist.size()>=1)
                {
                barRecord+="~*"+ ((DomText) page.getFirstByXPath("//span[@class='postal-code']/text()")).asText();
                }//end if
		else
		{
		     barRecord+="~*#NULL#";
		}//end else

                checkExsist = (List<DomNode>) page.getByXPath("//span[@id='bizPhone']");
                if(checkExsist.size()>=1)
                {
                barRecord+="~*"+ ((DomText) page.getFirstByXPath("//span[@id='bizPhone']/text()")).asText();
                }//endif
		else
		{
		     barRecord+="~*#NULL#";
		}//end else

                checkExsist = (List<DomNode>) page.getByXPath("//div[@id='BizUrl']");
                if(checkExsist.size()>=1)
                {
                barRecord+="~*"+((DomText) page.getFirstByXPath("//a[@class='url']/text()")).asText();
                }//end if
		else
		{
		     barRecord+="~*#NULL#";
		}//end else

                checkExsist = (List<DomNode>) page.getByXPath("//dt[@class='attr-BusinessHours']");
                if(checkExsist.size()>=1)
                {
                List<DomNode> theList = (List<DomNode>) page.getByXPath("//p[@class='hours']");                                
                barRecord+="~*";
                for(int z=0;z<theList.size();z++)
			{                                    
				barRecord+=((DomText) page.getFirstByXPath("//dd[@class='attr-BusinessHours']/p["+(z+1)+"]/text()")).asText();                                                       
			}//end for                 
                }//end if
		else
		{
		     barRecord+="~*#NULL#";
		}//end else

                checkExsist = (List<DomNode>) page.getByXPath("//dt[@class='attr-RestaurantsReservations']");
                if(checkExsist.size()>=1)
                {
                barRecord+="~*"+ ((DomText) page.getFirstByXPath("//dd[@class='attr-RestaurantsReservations']/text()")).asText();
                }//end if
		else
		{
		     barRecord+="~*#NULL#";
		}//end else

                checkExsist = (List<DomNode>) page.getByXPath("//dt[@class='attr-BusinessAcceptsCreditCards']");
                if(checkExsist.size()>=1)
                {
                barRecord+="~*"+((DomText) page.getFirstByXPath("//dd[@class='attr-BusinessAcceptsCreditCards']/text()")).asText();
                }//end if
		else
		{
		     barRecord+="~*#NULL#";
		}//end else


                checkExsist = (List<DomNode>) page.getByXPath("//dt[@class='attr-BusinessParking']");
                if(checkExsist.size()>=1)
                {
                barRecord+="~*"+ ((DomText) page.getFirstByXPath("//dd[@class='attr-BusinessParking']/text()")).asText();
                }//end if
		else
		{
		     barRecord+="~*#NULL#";
		}//end else
		

                checkExsist = (List<DomNode>) page.getByXPath("//dt[@class='attr-RestaurantsAttire']");
                if(checkExsist.size()>=1)
                {
                barRecord+="~*"+((DomText) page.getFirstByXPath("//dd[@class='attr-RestaurantsAttire']/text()")).asText();
                }//end if
		else
		{
		     barRecord+="~*#NULL#";
		}//end else

                checkExsist = (List<DomNode>) page.getByXPath("//dt[@class='attr-RestaurantsGoodForGroups']");
                if(checkExsist.size()>=1)
                {
                barRecord+="~*"+((DomText) page.getFirstByXPath("//dd[@class='attr-RestaurantsGoodForGroups']/text()")).asText();
                }//end if
		else
		{
		     barRecord+="~*#NULL#";
		}//end else

                checkExsist = (List<DomNode>) page.getByXPath("//dt[@class='attr-GoodForKids']");
                if(checkExsist.size()>=1)
                {
                barRecord+="~*"+((DomText) page.getFirstByXPath("//dd[@class='attr-GoodForKids']/text()")).asText();
                }//end if
		else
		{
		     barRecord+="~*#NULL#";
		}//end else

                checkExsist = (List<DomNode>) page.getByXPath("//dt[@class='attr-RestaurantsPriceRange2']");
                if(checkExsist.size()>=1)
                {
                barRecord+="~*"+((DomText) page.getFirstByXPath("//span[@id='price_tip']/text()")).asText();
                }//end if
		else
		{
		     barRecord+="~*#NULL#";
		}//end else

                checkExsist = (List<DomNode>) page.getByXPath("//dt[@class='attr-RestaurantsDelivery']");
                if(checkExsist.size()>=1)
                {
                barRecord+="~*"+((DomText) page.getFirstByXPath("//dd[@class='attr-RestaurantsDelivery']/text()")).asText();
                }//end if
		else
		{
		     barRecord+="~*#NULL#";
		}//end else

                checkExsist = (List<DomNode>) page.getByXPath("//dt[@class='attr-RestaurantsTakeOut']");
                if(checkExsist.size()>=1)
                {
                barRecord+="~*"+((DomText) page.getFirstByXPath("//dd[@class='attr-RestaurantsTakeOut']/text()")).asText();
                }//end if
		else
		{
		     barRecord+="~*#NULL#";
		}//end else

                checkExsist = (List<DomNode>) page.getByXPath("//dt[@class='attr-RestaurantsTableService']");
                if(checkExsist.size()>=1)
                {
                barRecord+="~*"+((DomText) page.getFirstByXPath("//dd[@class='attr-RestaurantsTableService']/text()")).asText();
                }//end if
		else
		{
		     barRecord+="~*#NULL#";
		}//end else

                checkExsist = (List<DomNode>) page.getByXPath("//dt[@class='attr-OutdoorSeating']");
                if(checkExsist.size()>=1)
                {
                barRecord+="~*"+((DomText) page.getFirstByXPath("//dd[@class='attr-OutdoorSeating']/text()")).asText();
                }//end if
		else
		{
		     barRecord+="~*#NULL#";
		}//end else

                checkExsist = (List<DomNode>) page.getByXPath("//dt[@class='attr-GoodForMeal']");
                if(checkExsist.size()>=1)
                {
                barRecord+="~*"+((DomText) page.getFirstByXPath("//dd[@class='attr-GoodForMeal']/text()")).asText();
                }//end if
		else
		{
		     barRecord+="~*#NULL#";
		}//end else

                checkExsist = (List<DomNode>) page.getByXPath("//dt[@class='attr-Music']");
                if(checkExsist.size()>=1)
                {
                barRecord+="~*"+((DomText) page.getFirstByXPath("//dd[@class='attr-Music']/text()")).asText();
                }//end if
		else
		{
		     barRecord+="~*#NULL#";
		}//end else

                checkExsist = (List<DomNode>) page.getByXPath("//dt[@class='attr-BestNights']");
                if(checkExsist.size()>=1)
                {
                barRecord+="~*"+((DomText) page.getFirstByXPath("//dd[@class='attr-BestNights']/text()")).asText();
                }//end if
		else
		{
		     barRecord+="~*#NULL#";
		}//end else

                checkExsist = (List<DomNode>) page.getByXPath("//dt[@class='attr-HappyHour']");
                if(checkExsist.size()>=1)
                {
                barRecord+="~*"+((DomText) page.getFirstByXPath("//dd[@class='attr-HappyHour']/text()")).asText();
                }//end if
		else
		{
		     barRecord+="~*#NULL#";
		}//end else

                checkExsist = (List<DomNode>) page.getByXPath("//dt[@class='attr-Alcohol']");
                if(checkExsist.size()>=1)
                {
                barRecord+="~*"+((DomText) page.getFirstByXPath("//dd[@class='attr-Alcohol']/text()")).asText();
                }//end if
		else
		{
		     barRecord+="~*#NULL#";
		}//end else

                checkExsist = (List<DomNode>) page.getByXPath("//dt[@class='attr-Smoking']");
                if(checkExsist.size()>=1)
                {
                barRecord+="~*"+ ((DomText) page.getFirstByXPath("//dd[@class='attr-Smoking']/text()")).asText();
                }//end if
		else
		{
		     barRecord+="~*#NULL#";
		}//end else

                checkExsist = (List<DomNode>) page.getByXPath("//dt[@class='attr-CoatCheck']");
                if(checkExsist.size()>=1)
                {
                barRecord+="~*"+((DomText) page.getFirstByXPath("//dd[@class='attr-CoatCheck']/text()")).asText();                                
                }//end if
		else
		{
		     barRecord+="~*#NULL#";
		}//end else

                checkExsist = (List<DomNode>) page.getByXPath("//dt[@class='attr-NoiseLevel']");
                if(checkExsist.size()>=1)
                {
                barRecord+="~*"+((DomText) page.getFirstByXPath("//dd[@class='attr-NoiseLevel']/text()")).asText();
                }//end if
		else
		{
		     barRecord+="~*#NULL#";
		}//end else

                checkExsist = (List<DomNode>) page.getByXPath("//dt[@class='attr-GoodForDancing']");
                if(checkExsist.size()>=1)
                {
                barRecord+="~*"+((DomText) page.getFirstByXPath("//dd[@class='attr-GoodForDancing']/text()")).asText();
                }//end if
		else
		{
		     barRecord+="~*#NULL#";
		}//end else

                checkExsist = (List<DomNode>) page.getByXPath("//dt[@class='attr-HasTV']");
                if(checkExsist.size()>=1)
                {
                barRecord+="~*"+((DomText) page.getFirstByXPath("//dd[@class='attr-HasTV']/text()")).asText();
                }//end if
		else
		{
		     barRecord+="~*#NULL#";
		}//end else

                checkExsist = (List<DomNode>) page.getByXPath("//dt[@class='attr-WheelchairAccessible']");
                if(checkExsist.size()>=1)
                {
                barRecord+="~*"+((DomText) page.getFirstByXPath("//dd[@class='attr-WheelchairAccessible']/text()")).asText();
                }//end if
		else
		{
		     barRecord+="~*#NULL#";
		}//end else

                checkExsist = (List<DomNode>) page.getByXPath("//dt[@class='attr-Ambience']");
                if(checkExsist.size()>=1)
                {
                    barRecord+="~*"+((DomText) page.getFirstByXPath("//dd[@class='attr-Ambience']/text()")).asText();
                }//end if
		else
		{
		     barRecord+="~*#NULL#";
		}//end else

                checkExsist = (List<DomNode>) page.getByXPath("//dt[@class='attr-DogsAllowed']");
                if(checkExsist.size()>=1)
                {
                    barRecord+="~*"+((DomText) page.getFirstByXPath("//dd[@class='attr-DogsAllowed']/text()")).asText();
                }//end if
		else
		{
		     barRecord+="~*#NULL#";
		}//end else

                checkExsist = (List<DomNode>) page.getByXPath("//dt[@class='attr-WiFi']");
                if(checkExsist.size()>=1)
                {
                    barRecord+="~*"+((DomText) page.getFirstByXPath("//dd[@class='attr-WiFi']/text()")).asText();
                }//end if
		else
		{
		     barRecord+="~*#NULL#";
		}//end else

                checkExsist = (List<DomNode>) page.getByXPath("//dt[@class='attr-Caters']");
                if(checkExsist.size()>=1)
                {
                    barRecord+="~*"+((DomText) page.getFirstByXPath("//dd[@class='attr-Caters']/text()")).asText();
                }//end if
		else
		{
		     barRecord+="~*#NULL#";
		}//end else
		
		
		vBrowser.closeAllWindows();
		System.out.println("Bar parsed");
		
		}//end try
		catch(FailingHttpStatusCodeException e)
		{
			System.out.println("Yelp recognized our IP in parseLink() on zipcode: " + zip.toString());
			barRecord = "#BadProxy#";
		}//end catch
		}
		catch(MalformedURLException e)
		{
			System.out.println("MAlformedURLExc in parseLink on zipcode: " + zip.toString());
		}//end catch
		
		
		
		return barRecord;

	}//end parseLink()
     
     
     private long getTime()
        {
            Date date = new Date();
            long time = date.getTime();
            return time;           
            
        }//end getDate
	
    
    
}//end class


