package com.actelion.research.chem.descriptor;

import com.actelion.research.chem.StereoMolecule;

public class DescriptorHandlerStandardFactory extends DescriptorHandlerStandard2DFactory {
    private static DescriptorHandlerStandardFactory sFactory;

    public static DescriptorHandlerFactory getFactory() {
        if (sFactory == null) {
        	synchronized(DescriptorHandlerStandardFactory.class) {
        		sFactory = new DescriptorHandlerStandardFactory();
        		}
        	}
        return sFactory;
        }

    public DescriptorHandler<Object, StereoMolecule> getDefaultDescriptorHandler(String shortName) {
        DescriptorHandler<Object, StereoMolecule> dh = super.getDefaultDescriptorHandler(shortName);
        if (dh != null)
            return dh;

        if (DESCRIPTOR_Flexophore.shortName.equals(shortName))
            return DescriptorHandlerFlexophore.getDefaultInstance();
        if (DESCRIPTOR_Flexophore_HighRes.shortName.equals(shortName))
            return DescriptorHandlerFlexophoreHighRes.getInstance();

        return null;
        }

    public DescriptorHandler<Object, StereoMolecule> create(String shortName) {
        DescriptorHandler dh = super.create(shortName);
        if (dh != null)
            return dh;

        if (DESCRIPTOR_Flexophore.shortName.equals(shortName))
            return new DescriptorHandlerFlexophore();
        if (DESCRIPTOR_Flexophore_HighRes.shortName.equals(shortName))
            return new DescriptorHandlerFlexophoreHighRes();

        return null;
        }
    }
