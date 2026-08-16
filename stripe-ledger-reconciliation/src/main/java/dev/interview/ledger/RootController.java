package dev.interview.ledger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
@Controller class RootController { @GetMapping(value={"/","/payments","/reconciliation"}) String root(){return "forward:/index.html";} }

