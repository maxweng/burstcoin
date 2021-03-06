package brs.http;

import brs.BurstException;
import brs.Order;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static brs.http.JSONResponses.UNKNOWN_ORDER;

public final class GetAskOrder extends APIServlet.APIRequestHandler {

  static final GetAskOrder instance = new GetAskOrder();

  private GetAskOrder() {
    super(new APITag[] {APITag.AE}, "order");
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {
    long orderId = ParameterParser.getOrderId(req);
    Order.Ask askOrder = Order.Ask.getAskOrder(orderId);
    if (askOrder == null) {
      return UNKNOWN_ORDER;
    }
    return JSONData.askOrder(askOrder);
  }

}
