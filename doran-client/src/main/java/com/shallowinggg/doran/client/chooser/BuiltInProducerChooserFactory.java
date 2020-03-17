package com.shallowinggg.doran.client.chooser;

import com.shallowinggg.doran.client.producer.BuiltInProducer;

/**
 * Specialization for {@link DefaultObjectChooserFactory} which
 * element type is {@link BuiltInProducer}.
 *
 * @author shallowinggg
 */
public class BuiltInProducerChooserFactory extends DefaultObjectChooserFactory<BuiltInProducer> {
    public static final BuiltInProducerChooserFactory INSTANCE = new BuiltInProducerChooserFactory();

    private BuiltInProducerChooserFactory() {}

    @Override
    public ObjectChooser<BuiltInProducer> newChooser(BuiltInProducer[] objects) {
        return super.newChooser(objects);
    }
}
