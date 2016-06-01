package es.keensoft.alfresco.opencmis;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.opencmis.CMISConnector;
import org.alfresco.opencmis.dictionary.PropertyDefinitionWrapper;
import org.alfresco.opencmis.dictionary.TypeDefinitionWrapper;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.Pair;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.CmisExtensionElement;
import org.apache.chemistry.opencmis.commons.data.Properties;
import org.apache.chemistry.opencmis.commons.data.PropertyData;
import org.apache.chemistry.opencmis.commons.enums.Cardinality;
import org.apache.chemistry.opencmis.commons.enums.Updatability;
import org.apache.chemistry.opencmis.commons.exceptions.CmisInvalidArgumentException;

public class PatchedCMISConnector extends CMISConnector {

	@Override
    public void setProperties(NodeRef nodeRef, TypeDefinitionWrapper type, Properties properties, String... exclude)
    {
        if (properties == null)
        {
            return;
        }
        
        Map<String, PropertyData<?>> incomingPropsMap = properties.getProperties();
        if (incomingPropsMap == null)
        {
            return;
        }

        // extract property data into an easier to use form
        Map<String, Pair<TypeDefinitionWrapper, Serializable>> propsMap = new HashMap<String, Pair<TypeDefinitionWrapper, Serializable>>();
        for (String propertyId : incomingPropsMap.keySet())
        {
            PropertyData<?> property = incomingPropsMap.get(propertyId);
            PropertyDefinitionWrapper propDef = type.getPropertyById(property.getId());
            if (propDef == null)
            {
                propDef = getOpenCMISDictionaryService().findProperty(propertyId);
                if (propDef == null)
                {
                    throw new CmisInvalidArgumentException("Property " + property.getId() + " is unknown!");
                }
            }

            Updatability updatability = propDef.getPropertyDefinition().getUpdatability();
            if ((updatability == Updatability.READONLY)
                    || (updatability == Updatability.WHENCHECKEDOUT && !getCheckOutCheckInService().isWorkingCopy(nodeRef)))
            {
                throw new CmisInvalidArgumentException("Property " + property.getId() + " is read-only!");
            }
            TypeDefinitionWrapper propType = propDef.getOwningType();
            Serializable value = getValue(property, propDef.getPropertyDefinition().getCardinality() == Cardinality.MULTI);
            Pair<TypeDefinitionWrapper, Serializable> pair = new Pair<TypeDefinitionWrapper, Serializable>(propType, value);
            propsMap.put(propertyId, pair);
        }

        // Need to do deal with secondary types first
        Pair<TypeDefinitionWrapper, Serializable> pair = propsMap.get(PropertyIds.SECONDARY_OBJECT_TYPE_IDS);
        Serializable secondaryTypesProperty = (pair != null ? pair.getSecond() : null);
        if(secondaryTypesProperty != null)
        {
            if (!(secondaryTypesProperty instanceof List))
            {
                throw new CmisInvalidArgumentException("Secondary types must be a list!");
            }
            List secondaryTypes = (List)secondaryTypesProperty;
            if(secondaryTypes != null && secondaryTypes.size() > 0)
            {
                // add/remove secondary types/aspects
                processSecondaryTypes(nodeRef, secondaryTypes, propsMap);
            }
        }

        for (String propertyId : propsMap.keySet())
        {
            if(propertyId.equals(PropertyIds.SECONDARY_OBJECT_TYPE_IDS))
            {
                // already handled above
                continue;
            }

            // Only if property is passed as parameter
            if (incomingPropsMap.get(propertyId) != null) {
            	
            	pair = propsMap.get(propertyId);
	            TypeDefinitionWrapper propType = pair.getFirst();
	            Serializable value = pair.getSecond();
	            if (Arrays.binarySearch(exclude, propertyId) < 0)
	            {
	                setProperty(nodeRef, propType, propertyId, value);
	            }
	            
            }
        }

        List<CmisExtensionElement> extensions = properties.getExtensions();
        if (extensions != null)
        {
        	boolean isNameChanging = properties.getProperties().containsKey(PropertyIds.NAME);

            for (CmisExtensionElement extension : extensions)
            {
                if (ALFRESCO_EXTENSION_NAMESPACE.equals(extension.getNamespace())
                        && SET_ASPECTS.equals(extension.getName()))
                {
                    setAspectProperties(nodeRef, isNameChanging, extension);
                    break;
                }
            }
        }
    }
	
    private Serializable getValue(PropertyData<?> property, boolean isMultiValue)
    {
        if ((property.getValues() == null) || (property.getValues().isEmpty()))
        {
            return null;
        }

        if (isMultiValue)
        {
            return (Serializable) property.getValues();
        }

        return (Serializable) property.getValues().get(0);
    }
	
    private void processSecondaryTypes(NodeRef nodeRef, List secondaryTypes, Map<String, Pair<TypeDefinitionWrapper, Serializable>> propsToAdd) {
    	try {
	        Method m = getClass().getSuperclass().getDeclaredMethod("processSecondaryTypes", new Class<?>[]{NodeRef.class, List.class, Map.class});
	        m.setAccessible(true);
	        m.invoke(this, new Object[]{nodeRef, secondaryTypes, propsToAdd});
    	} catch (Exception e) {
    		throw new RuntimeException(e);
    	}
    }
	
    private void setAspectProperties(NodeRef nodeRef, boolean isNameChanging, CmisExtensionElement aspectExtension) {
    	try {
	        Method m = getClass().getSuperclass().getDeclaredMethod("setAspectProperties", new Class<?>[]{NodeRef.class, boolean.class, CmisExtensionElement.class});
	        m.setAccessible(true);
	        m.invoke(this, new Object[]{nodeRef, isNameChanging, aspectExtension});
    	} catch (Exception e) {
    		throw new RuntimeException(e);
    	}
    }


}
