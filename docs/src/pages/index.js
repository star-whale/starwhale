/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 * @format
 */

import React from "react";
import clsx from "clsx";
import Layout from "@theme/Layout";
import Link from "@docusaurus/Link";
import useDocusaurusContext from "@docusaurus/useDocusaurusContext";
import useBaseUrl from "@docusaurus/useBaseUrl";
import styles from "./styles.module.css";
import "./index.scss";
import CodeBlock from "@theme/CodeBlock";
import Tabs from "@theme/Tabs";
import TabItem from "@theme/TabItem";
import "/static/fonts/iconfont.js";
import tabSectionProject from "!!raw-loader!./tabSectionProject.sh";
import tabSectionDataset from "!!raw-loader!./tabSectionDataset.sh";

const swTabs = [
  {
    tab: "Starwhale Project",
    code: (
      <CodeBlock className="tabs__code" language="shell">
        {tabSectionProject}
      </CodeBlock>
    ),
    desc: (
      <>
        You can creat one or several projects for a data scientist team, a
        product line or a specific model.
        <p>
          Every user in cloud instances has their personal project by default.
        </p>
      </>
    ),
    button: {
      label: "Learn more",
      to: "/docs",
    },
  },
  {
    tab: "Starwhale Model",
    code: (
      <CodeBlock className="tabs__code" language="shell">
        1
      </CodeBlock>
    ),
    desc: (
      <>
       Starwhale Model is the standard model format used in model delivery.
       <p>
       It is a directory containing arbitrary files, including the model generated 
      by ML frameworks, the code to run the model, the metadata defined by Starwhale, and many other files.
      </p>
      </>
    ),
    button: {
      label: "Learn more",
      to: "/docs",
    },
  },
  {
    tab: "Starwhale Dataset",
    code: (
      <CodeBlock className="tabs__code" language="shell">
        {tabSectionDataset}
      </CodeBlock>
    ),
    desc: (
      <>
        Starwhale public dataset：starwhale provides massive public dataset.
        <p>
        Easy to work：difine dataset.yaml, use swcli comand can easily generate
        the dataset.
       </p>
      </>
    ),
    button: {
      label: "Learn more",
      to: "/docs",
    },
  },
];

const sectionUsers = [
  {
    image: "img/enterprise.svg",
    title: "Enterprise",
    description:
      "Organizations use Starwhale to track all the organization's machine learning projects, manage access controls and reproduce more efficiently.",
  },
  {
    image: "img/team.svg",
    title: "Team",
    description:
      "Teams use Starwhale to standardize team's projects, share projects' updates and improve productivity.",
  },
  {
    image: "img/individuals.svg",
    title: "Individuals",
    description:
      "Individuals use Starwhale to track, compare and evaluate models. The visible metics and exact dataset versions build model better.",
  },
];

const sectionsIntegratFrameworks = [
  {
    icon: "",
    title: "TensorFlow",
  },
  {
    icon: "img/intergrate_PyTorch.svg",
    title: "PyTorch",
  },
  {
    icon: "",
    title: "Keras",
  },
  {
    icon: "",
    title: "XGBoost",
  },
  {
    icon: "",
    title: "Kubeflow",
  },
  {
    icon: "",
    title: "Kubernetes",
  },
  {
    icon: "",
    title: "Python",
  },
  {
    icon: "",
    title: "MXNet",
  },
];

function Home() {
  const context = useDocusaurusContext();
  const { siteConfig = {} } = context;
  const { customFields = {} } = siteConfig;

  return (
    <Layout
      title={`Hello from ${siteConfig.title}`}
      description="Description will go into a meta tag in <head />"
    >
      <header className={clsx("hero hero--primary", styles.heroBanner)}>
        <div className="container">
          <h1 className={styles.heroProjectTagline}>
            <span
              className={styles.heroTitleTextHtml}
              dangerouslySetInnerHTML={{
                __html: siteConfig.tagline,
              }}
            />
          </h1>
          <div className={styles.indexCtas}>
            <Link
              className="button button--primary button--secondary button--lg button--rounded"
              to="/docs"
            >
              Get started
            </Link>
            <Link
              className="button button--outline button--secondary button--lg button--rounded"
              to="https://app.starwhale.ai"
            >
              Request a Demo
            </Link>
            {/* <span className={styles.indexCtasGitHubButtonWrapper}>
              <iframe
                className={styles.indexCtasGitHubButton}
                src="https://ghbtns.com/github-btn.html?user=star-whale&amp;repo=starwhale&amp;type=star&amp;count=true&amp;size=large"
                width={160}
                height={30}
                title="GitHub Stars"
              />
            </span> */}
          </div>
        </div>
      </header>
      <main className="swMain">
        {/* starwhale tabs show case of project/model/dataset/runtime */}
        <section className="card swTabs">
          <div className="card__body">
            <Tabs className="tabs tabs--block">
              {swTabs.map((item, index) => (
                <TabItem
                  key={index}
                  value={item.tab}
                  label={item.tab}
                  default={index === 0 ? true : undefined}
                >
                  <div className="tabs__body">
                    {item.code}
                    <div className="tabs__desc " id="project">
                      <span>{item.desc}</span>
                      <Link
                        className="button button--primary  button--rounded"
                        to={item.button?.to}
                      >
                        {item.button?.label}
                      </Link>
                    </div>
                  </div>
                </TabItem>
              ))}
            </Tabs>
          </div>
        </section>
        {/* starwhale users */}
        <section className="swUser">
          <h1>Who is Starwhale for?</h1>
          <div className="divider">
            <span className="iconfont icon-value" />
          </div>
          <div className="user__body">
            {sectionUsers.map((user, index) => (
              <div className="card" key={index}>
                <div className="card__image">
                  <div
                    style={{
                      backgroundImage: `url(${useBaseUrl(user.image)})`,
                      backgroundSize: "100%",
                      width: "100%",
                      height: "100%",
                      backgroundColor: "transparent",
                    }}
                    alt="Image alt text"
                  />
                </div>
                <div className="card__body">
                  <h3>{user.title}</h3>
                  <div className="divider2">
                    <span className="iconfont icon-line icon-blue" />
                  </div>
                  <span>{user.description}</span>
                </div>
              </div>
            ))}
          </div>
        </section>
        {/* starwhale try now */}
        <section className="swIntegrate">
          <h1>Starwhale integrates with any framework</h1>
          <div className="divider">
            <span className="iconfont icon-integrates" />
          </div>
          <div className="integrate integrate--8">
            <div
              className="integrate__bg map map--8"
              style={{
                background: `
                url(${useBaseUrl("img/line5-top.svg")}) center top no-repeat,
                url(${useBaseUrl(
                  "img/line3-down.svg"
                )}) center bottom no-repeat;
                `,
              }}
            >
              <div className="map__center map__item--center">
                <div className="map__center--logo">
                  <img
                    src={useBaseUrl("img/starwhale.png")}
                    alt="Starwhale"
                    height="20px"
                  />
                </div>
              </div>
            </div>
            <div className="integrate__body">
              {sectionsIntegratFrameworks.map((item, index) => (
                <div className="map__item" key={index}>
                  {item.icon && (
                    <img
                      src={useBaseUrl(item.icon)}
                      alt={item.title}
                      height="24px"
                    />
                  )}
                  {item.icon && <div className="map__divider"></div>}
                  <span>{item.title}</span>
                </div>
              ))}
            </div>
          </div>
        </section>
        <section className="swTry">
          <h1>Try today</h1>
          <div className="try">
            <div className="try__item">
              <span>
                <b>If</b> you want to know more detail about starwhale just
                click the button "get started" started"
              </span>
              <Link
                className="button button--primary  button--rounded"
                to="/docs"
              >
                Get started
              </Link>
            </div>
            <div className="try__item">
              <span>
                <b>If</b> you want more information or leave a message to us
                just click the button "contact us"
              </span>
              <Link
                className="button button--primary  button--rounded"
                to="/docs"
              >
                Contact us
              </Link>
            </div>
          </div>
        </section>
      </main>
    </Layout>
  );
}

export default Home;
