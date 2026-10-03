package de.emphasize.time2.storage.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/overview/")
public class OverviewController {

  @GetMapping(value = "/")
  public String getOverview() {
    /**
     * db->loadLayoutAndChanged(time() * 1000, $layout, $layoutChanged); $db->loadEventDays($days);
     * $db->loadInvoiceChecksums($invoiceChecksums); $c = json_encode(['layout' => $layout,
     * 'layoutChanged' => +$layoutChanged, 'eventDays' => $days, 'invoiceChecksums' =>
     * $invoiceChecksums]); header("status: 200"); header('Content-Type:
     * application/json;charset=utf-8;'); echo ($c);
     */
    return "{\"some\": \"example\"}";
  }
}
