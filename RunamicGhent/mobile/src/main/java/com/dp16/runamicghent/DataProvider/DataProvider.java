/*
 * Copyright (c) 2017 Hendrik Depauw
 * Copyright (c) 2017 Lorenz van Herwaarden
 * Copyright (c) 2017 Nick Aelterman
 * Copyright (c) 2017 Olivier Cammaert
 * Copyright (c) 2017 Maxim Deweirdt
 * Copyright (c) 2017 Gerwin Dox
 * Copyright (c) 2017 Simon Neuville
 * Copyright (c) 2017 Stiaan Uyttersprot
 *
 * This software may be modified and distributed under the terms of the MIT license.  See the LICENSE file for details.
 */

package com.dp16.runamicghent.DataProvider;

/**
 * Interface that should be implemented by all dataproviders
 * Provide a start, resume, pause and stop method
 * Stop is the counterpart of start, they should only be called once.
 * Resume is the counterpart of pause and can be called anytime.
 * Created by hendrikdepauw on 08/03/2017.
 */

public interface DataProvider {
    void start();

    void stop();

    void resume();

    void pause();
}
