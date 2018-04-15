package it.polito.mad.greit.project;

public class ISBNValidator {
  private String ISBN;
  
  public ISBNValidator(String ISBN) {
    this.ISBN = ISBN;
  }
  
  public boolean isValid() {
    if (ISBN == null) {
      return false;
    }
    
    //remove any hyphens
    ISBN = ISBN.replaceAll("-", "");
    
    //must be a 13 digit ISBN
    if (ISBN.length() != 13) {
      return false;
    }
    
    try {
      int tot = 0;
      for (int i = 0; i < 12; i++) {
        int digit = Integer.parseInt(ISBN.substring(i, i + 1));
        tot += (i % 2 == 0) ? digit * 1 : digit * 3;
      }
      
      //checksum must be 0-9. If calculated as 10 then = 0
      int checksum = 10 - (tot % 10);
      if (checksum == 10) {
        checksum = 0;
      }
      
      return checksum == Integer.parseInt(ISBN.substring(12));
    } catch (NumberFormatException nfe) {
      //to catch invalid ISBNs that have non-numeric characters in them
      return false;
    }
  }
}
