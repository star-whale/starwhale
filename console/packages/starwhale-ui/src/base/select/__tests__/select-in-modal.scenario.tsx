/*
Copyright (c) Uber Technologies, Inc.

This source code is licensed under the MIT license found in the
LICENSE file in the root directory of this source tree.
*/
import React, { useState } from 'react';
import { Button } from '../../button';
import { StatefulSelect } from '..';
import { Modal, ModalHeader, ModalBody, ModalFooter, ModalButton } from '../../modal';

export function Scenario() {
  const [isOpen, setIsOpen] = useState(false);
  return (
    <>
      <Button
        onClick={() => {
          setIsOpen(true);
        }}
      >
        Open Modal
      </Button>
      <Modal
        onClose={() => {
          setIsOpen(false);
        }}
        isOpen={isOpen}
      >
        <ModalHeader>Hello world</ModalHeader>
        <ModalBody>
          <StatefulSelect
            options={[
              { id: 'AliceBlue', color: '#F0F8FF' },
              { id: 'AntiqueWhite', color: '#FAEBD7' },
              { id: 'Aqua', color: '#00FFFF' },
              { id: 'Aquamarine', color: '#7FFFD4' },
              { id: 'Azure', color: '#F0FFFF' },
              { id: 'Beige', color: '#F5F5DC' },
            ]}
            labelKey="id"
            valueKey="color"
          />
        </ModalBody>
        <ModalFooter>
          <ModalButton
            onClick={() => {
              setIsOpen(false);
            }}
          >
            Cancel
          </ModalButton>
          <ModalButton
            onClick={() => {
              setIsOpen(false);
            }}
          >
            Okay
          </ModalButton>
        </ModalFooter>
      </Modal>
    </>
  );
}
