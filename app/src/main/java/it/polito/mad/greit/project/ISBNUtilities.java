package it.polito.mad.greit.project;

public class ISBNUtilities {
  private String ISBN;
  
  public ISBNUtilities(String ISBN) {
    this.ISBN = ISBN.replaceAll("-", "")
        .replaceAll(" ", "");
  }
  
  public boolean isValid() {
    if (ISBN == null) {
      return false;
    }
    
    //remove any hyphens
    ISBN = ISBN.replaceAll("-", "");
    
    switch (ISBN.length()) {
      case 13:
        return isValid13();
      
      case 10:
        return isValid10();
      
      default:
        return false;
    }
  }
  
  private boolean isValid10() {
    try {
      int tot = 0;
      for (int i = 0; i < 9; i++) {
        int digit = Integer.parseInt(this.ISBN.substring(i, i + 1));
        tot += ((10 - i) * digit);
      }
      
      String checksum = Integer.toString((11 - (tot % 11)) % 11);
      if ("10".equals(checksum)) {
        checksum = "X";
      }
      
      return checksum.equals(this.ISBN.substring(9));
    } catch (NumberFormatException nfe) {
      //to catch invalid ISBNs that have non-numeric characters in them
      return false;
    }
  }
  
  private boolean isValid13() {
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
  
  public String convertToISBN13() {
    if (ISBN.length() == 13)
      return this.ISBN;
    
    ISBN = ISBN.substring(0, 9);
    ISBN = "978" + ISBN;
    
    int checkValue = 0;
  
    for (int i = 0; i < ISBN.length(); i++){
      char c = ISBN.charAt(i);
      
      if ((i % 2) == 0) {
        checkValue += Integer.valueOf(c) - 48;
      } else {
        checkValue += (Integer.valueOf(c) - 48) * 3;
      }
    }
    
    checkValue = (10 - (checkValue % 10)) % 10;
    
    return ISBN + String.valueOf(checkValue);
  }
}
