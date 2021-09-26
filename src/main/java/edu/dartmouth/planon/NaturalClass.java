package edu.dartmouth.planon;

import nl.planon.hades.userextension.uxinterface.*;
import nl.planon.util.pnlogging.PnLogger;

// Class to get the natural class given
public class NaturalClass implements IUserExtension{

    private static final PnLogger LOG = PnLogger.getLogger(NaturalClass.class);

    // Set description
    private String description = "This solution extension returns natural class";

    public NaturalClass() {}

    public void executeUX (IUXBusinessObject currentBO, IUXBusinessObject oldBO, IUXContext context, String parameter) {
        LOG.info("--------------------------------------------");
        LOG.info("SX " + getClass().getName() + " starting..." + NaturalClass.class.getName());

        // Order lines
        // Get the chart string
        String chartString = currentBO.getStringFieldByName("FreeString11").getValueAsString();
        LOG.info("Chartstring: " + chartString);

        // Get the cost type
        String costType = currentBO.getCodesCodeNameFieldByName("FreeString12").getValueAsString();
        LOG.info("Cost type: " + costType);

        // Get the transaction type
        String transactionType = currentBO.getCodesNameFieldByName("FreeString13").getValueAsString();
        LOG.info("Transaction type: " + transactionType);

        // Get the work order method
        String woMethod = currentBO.getCodesNameFieldByName("FreeString10").getValueAsString();
        LOG.info("WO method: " + woMethod);

        // Get the trade (crew) via Order BO
        IUXReferenceField orderReferenceField = currentBO.getReferenceFieldByName("OrderRef");
        IUXBusinessObject order = orderReferenceField.getValueAsBO();
        IUXReferenceField tradeReferenceField = order.getReferenceFieldByName("TradeRef");
        IUXBusinessObject trade = tradeReferenceField.getValueAsBO();
        Integer tradePrimaryKey = trade.getPrimaryKey();
        LOG.info("Trade Primary Key: " + tradePrimaryKey);

        // Get entity
        String entity = chartString.substring(0,2);
        Integer entityInt = Integer.parseInt(entity);
        if (entityInt < 50){
            entity = "Internal < 50";
        } else{
            entity = "External ≥ 50";
        }
        LOG.info("Entity: " + entity);

        // Get org
        String org = chartString.substring(2, 5);
        if (!org.equals("003")){
            org = "All else";
        }
        LOG.info("Org: " + org);

        // Get natural class from the mapping table
        Integer naturalClass = calculateNaturalClass(woMethod, costType, tradePrimaryKey, entity, org, transactionType, context);
        LOG.info("Natural Class: " + naturalClass);

        // Set Natural Class on order lines
        IUXIntegerField nClass = currentBO.getIntegerFieldByName("FreeInteger1");
        nClass.setValueAsInteger(naturalClass);

        // Save changes
        currentBO.executeFieldChanges();
    }

    public Integer calculateNaturalClass (String woMethod, String costType, Integer tradePrimaryKey, String entity, String org, String transactionType, IUXContext context){
        // Calculate natural class

        // Find the UsrNaturalClassMapping BO
        IUXDatabaseQueryBuilder queryBldr = context.getBODatabaseQueryBuilder("UsrNaturalClassMapping");
        queryBldr.addSelectField("Syscode");
        queryBldr.addSearchField("FreeInteger1"); // Trade
        queryBldr.addSearchField("FreeString1"); // Org
        queryBldr.addSearchField("FreeString21"); // Entity
        queryBldr.addSearchField("FreeString22"); // Transaction type
        queryBldr.addSearchField("FreeString23"); // Work order method
        queryBldr.addSearchField("FreeString3"); // Cost type
        IUXDatabaseQuery query = queryBldr.build();
        query.getIntegerSearchExpression("FreeInteger1", UXOperator.EQUAL).setValue(tradePrimaryKey);
        query.getStringSearchExpression("FreeString1", UXOperator.EQUAL).setValue(org);
        query.getStringSearchExpression("FreeString21", UXOperator.EQUAL).setValue(entity);
        query.getStringSearchExpression("FreeString22", UXOperator.EQUAL).setValue(transactionType);
        query.getStringSearchExpression("FreeString23", UXOperator.EQUAL).setValue(woMethod);
        query.getStringSearchExpression("FreeString3", UXOperator.EQUAL).setValue(costType);
        IUXResultSet result = query.executeAll();
        Integer naturalClass = 0000;
        if (result.first()) {
            Integer usrNaturalClassMappingRef = result.getPrimaryKey();
            IUXBusinessObject usrNaturalClassMapping = context.getBOByPrimaryKey("UsrNaturalClassMapping", usrNaturalClassMappingRef);
            naturalClass = usrNaturalClassMapping.getIntegerFieldByName("FreeInteger4").getValueAsInteger();
        }
        return naturalClass;
    }

    // Return description
    public String getDescription() {
        return description;
    }
}
